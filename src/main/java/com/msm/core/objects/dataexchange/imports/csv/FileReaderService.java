package com.msm.core.objects.dataexchange.imports.csv;

import com.github.pjfanning.xlsx.StreamingReader;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.dataexchange.FileType;
import com.msm.core.objects.dataexchange.imports.model.FileProcessStatus;
import com.msm.core.objects.dataexchange.imports.model.FileProcessedEventContext;
import com.msm.core.objects.dataexchange.imports.model.RawRow;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class FileReaderService {
    private final ActionExecutor actionExecutor;


    public void readCsv(String importObjectName, UUID importId, String fileUrl, int bufferSize, Consumer<RawRow<CSVRecord>> consumer) {

        FileProcessStatus status = FileProcessStatus.PROCESSING;
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE);

        try (BOMInputStream bomInputStream = BOMInputStream.builder().setURI(URI.create(fileUrl)).get();
             InputStreamReader isr = new InputStreamReader(bomInputStream, decoder);
             BufferedReader reader = new BufferedReader(isr, bufferSize)
        ) {
            CSVFormat csvFormat = CsvDelimiterDetector.detect(
                    reader,
                    bufferSize
            );

            try (CSVParser csvParser = csvFormat.parse(reader)) {

                for (CSVRecord csvRecord : csvParser) {
                    consumer.accept(
                            new RawRow<>(csvRecord.getRecordNumber(), importObjectName, csvRecord)
                    );
                }
            }

        } catch (Exception e) {
            status = FileProcessStatus.SUCCESS;
            throw Lombok.sneakyThrow(e);
        } finally {
            finishFileProcess(importObjectName, importId, FileType.CSV, status);
        }
    }

    public void readExcel(String importObjectName, UUID importId, String fileUrl, Consumer<RawRow<Row>> consumer) {
//        Workbook workbook = null;
//        InputStream inputStream = null;
        FileProcessStatus status = FileProcessStatus.PROCESSING;
        try {
            URL url = URI.create(fileUrl).toURL();
            try (
                    InputStream inputStream = url.openStream();
                    Workbook workbook = WorkbookFactory.create(inputStream)
            ) {

                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    consumer.accept(
                            new RawRow<>(row.getRowNum(), importObjectName, row)
                    );
                }
                status = FileProcessStatus.SUCCESS;
                Path outputFile = Files.createTempFile(UUID.randomUUID().toString(), ".xlsx");
                try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
                    workbook.write(outputStream);
                }
            }
        } catch (Exception e) {
            status = FileProcessStatus.SUCCESS;
            throw Lombok.sneakyThrow(e);
        } finally {
            finishFileProcess(importObjectName, importId, FileType.EXCEL, status);
        }
    }


    public void readExcelStream(String importObjectName, UUID importId, String fileUrl, int bufferSize, int batchSize, Consumer<RawRow<Row>> consumer) {
        Workbook workbook = null;
        InputStream inputStream = null;
        try {
            URL url = URI.create(fileUrl).toURL();
            inputStream = url.openStream();
            workbook = StreamingReader.builder()
                    .rowCacheSize(batchSize)           // Data hold in RAM (Buffer Size)
                    .bufferSize(bufferSize)            // Buffer size of InputStream (Bytes)
                    .open(inputStream);

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if(isRowEmpty(row)) continue;
                consumer.accept(
                        new RawRow<>(row.getRowNum(), importObjectName, row)
                );
            }
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        } finally {
            if (Objects.nonNull(workbook)) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Error while close workbook: {}", e.getMessage(), e);
                }
            }
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error while close inputStream: {}", e.getMessage(), e);
                }
            }
        }
    }

    private void finishFileProcess(String importObjectName, UUID importId, FileType fileType, FileProcessStatus status) {

        FileProcessedEventContext.of(importObjectName, importId, fileType, status);
        ActionContext<FileProcessedEventContext> actionRequest = ActionContext
                .<FileProcessedEventContext>builder()
                .resource(importObjectName)
                .action(ObjectActionNamed.Excel.FILE_PROCESSED_EVENT)
                .payload(FileProcessedEventContext.of(importObjectName, importId, fileType, status))
                .build();

        actionExecutor.execute(actionRequest);
    }

    public static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (cell.getCellType() == CellType.STRING) {
                    if (!cell.getStringCellValue().trim().isEmpty()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}

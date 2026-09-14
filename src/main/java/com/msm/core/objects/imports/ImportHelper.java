package com.msm.core.objects.imports;

import com.github.pjfanning.xlsx.StreamingReader;
import com.msm.core.commons.Utils;
import com.msm.core.objects.imports.model.RawRow;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public final class ImportHelper {
    private ImportHelper(){}

    public static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();

            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    yield (long) value;
                }

                yield value;
            }

            case BOOLEAN -> cell.getBooleanCellValue();

            case FORMULA -> getFormulaCellValue(cell);

            case ERROR -> cell.getErrorCellValue();

            default -> null;
        };
    }


    public static Object getFormulaCellValue(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();

            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }

                double value = cell.getNumericCellValue();

                if (value == Math.floor(value)) {
                    yield (long) value;
                }

                yield value;
            }

            case BOOLEAN -> cell.getBooleanCellValue();

            case ERROR -> cell.getErrorCellValue();

            default -> null;
        };
    }

    public static void readExcelStream(String importObjectName, String fileUrl, int bufferSize, int batchSize, Consumer<RawRow<Row>> consumer) {
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

    public static List<String> arrayParser(String resource) {
        if (Utils.STR.isBlank(resource)) {
            return Utils.CL.newArrayList();
        }
        return  Arrays.asList(resource.replaceAll("[{}]", "").split(","));
    }
}

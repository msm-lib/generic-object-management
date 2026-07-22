package com.msm.core.objects.service.imports;

import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.service.imports.mapper.RowMapper;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MultipartCsvObjectReader implements FileReader<MultipartFile, Map<String, Object>> {
    private final GenericObjectConfigProperties genericObjectConfigProperties;
    private final RowMapper<RowMapperContext, Map<String, Object>> csvRowMapper;
    private final BatchExecutionService batchExecutionService;


    public List<Map<String, Object>> read(String objectName, MultipartFile multipartFile) {
        List<Map<String, Object>> items = new LinkedList<>();
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE);
        int BATCH_SIZE = genericObjectConfigProperties.getImportFile().getBatchSize();
        int BUFFER_SIZE = genericObjectConfigProperties.getImportFile().getBufferSize();

        try (BOMInputStream bomInputStream = BOMInputStream.builder()
                .setInputStream(multipartFile.getInputStream())
                .get();
             InputStreamReader isr = new InputStreamReader(bomInputStream, decoder);
             BufferedReader reader = new BufferedReader(isr, BUFFER_SIZE)
        ) {
            CSVFormat csvFormat = CsvDelimiterDetector.detect(reader, BUFFER_SIZE);
            try(CSVParser csvParser = csvFormat.parse(reader)) {
                for (CSVRecord csvRecord : csvParser) {
                    RowMapperContext context = RowMapperContext
                            .builder()
                            .row(csvRecord)
                            .objectName(objectName)
                            .build();
                    Map<String, Object> item = csvRowMapper.mapRow(context);
                    items.add(item);

                    if(items.size() >= BATCH_SIZE) {
                        batchExecutionService.batchImportAndCleanup(objectName, items);
                    }
                }

                if(!items.isEmpty()) {
                    batchExecutionService.batchImportAndCleanup(objectName, items);
                }
            }


        } catch (Exception ex) {
            throw Lombok.sneakyThrow(ex);
        }

        return items;
    }
}

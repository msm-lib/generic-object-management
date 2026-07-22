package com.msm.core.objects.service.imports;

import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.service.imports.mapper.RowMapper;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CsvObjectReader implements FileReader<String, Map<String, Object>> {
    private final GenericObjectConfigProperties genericObjectConfigProperties;
    private final RowMapper<RowMapperContext, Map<String, Object>> csvRowMapper;
    private final BatchExecutionService batchExecutionService;


    public List<Map<String, Object>> read(String objectName, String urlString) {
        int BATCH_SIZE = genericObjectConfigProperties.getImportFile().getBatchSize();
        List<Map<String, Object>> items = new LinkedList<>();

        try {
            Path path = Paths.get(urlString);
            BufferedReader reader = Files.newBufferedReader(path);

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get()
                    .parse(reader);

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
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw Lombok.sneakyThrow(e);
        }
        return items;
    }
}

package com.msm.core.objects.service.imports;

import com.msm.core.metadata.Attribute;
import com.msm.core.objects.service.imports.mapper.RowMapper;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class MultipartCsvObjectReader implements FileReader<MultipartFile, Map<String, Object>> {
    private static final long MAX_CODE_IMPORT = 100;
    private final RowMapper<RowMapperContext, Map<String, Object>> csvRowMapper;
    private final BatchExecutionService batchExecutionService;


    public List<Map<String, Object>> read(String objectName, MultipartFile multipartFile) {
        List<Map<String, Object>> items = new LinkedList<>();
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE);

        try (InputStreamReader isr = new InputStreamReader(multipartFile.getInputStream(), decoder);
             BufferedReader reader = new BufferedReader(isr);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(";")
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .get()
                     .parse(reader)) {

            Map<Attribute, Set<String>> attrCodeMap = new HashMap<>();
            for (CSVRecord csvRecord : csvParser) {
                RowMapperContext context = RowMapperContext
                        .builder()
                        .row(csvRecord)
                        .objectName(objectName)
                        .attrCodeMap(attrCodeMap)
                        .build();
                Map<String, Object> item = csvRowMapper.mapRow(context);
                items.add(item);

                if(items.size() >= MAX_CODE_IMPORT) {
                    batchExecutionService.batchImportAndCleanup(objectName, items, attrCodeMap);
                }
            }

            if(!items.isEmpty()) {
                batchExecutionService.batchImportAndCleanup(objectName, items, attrCodeMap);
            }

        } catch (Exception ex) {

//            log.error(e.getMessage(), e);
            throw Lombok.sneakyThrow(ex);
        }

        return items;
    }
}

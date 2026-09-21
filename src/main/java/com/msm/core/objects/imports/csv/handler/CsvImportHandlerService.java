package com.msm.core.objects.imports.csv.handler;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.imports.BatchValidationService;
import com.msm.core.objects.imports.FileReaderService;
import com.msm.core.objects.imports.ImportConfigService;
import com.msm.core.objects.imports.ImportHelper;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import com.msm.core.objects.imports.model.BatchRowData;
import com.msm.core.objects.imports.model.CellMappingContext;
import com.msm.core.objects.imports.model.ReadActionContext;
import com.msm.core.objects.imports.model.RowMappingContext;
import com.msm.core.objects.imports.reference.AttributeRefHelper;
import com.msm.core.objects.imports.reference.AttributeReferenceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class CsvImportHandlerService {
    private static final Set<String> IGNORE_ATTRIBUTE = Set.of("customValues");
    private final BatchValidationService batchValidationService;
    private final FileReaderService fileReaderService;
    private final AttributeReferenceResolver attributeReferenceResolver;
    private final ImportConfigService importConfigService;

    @Handler(action = ObjectActionNamed.Csv.READ_FILE)
    public void readData(ActionContext<ReadActionContext<CSVRecord>> actionContext) {
        ReadActionContext<CSVRecord> readActionContext = actionContext.getPayload();
        ObjectImportRegistry.ProcessingConfig objectConfig = importConfigService.getProcessingConfig(actionContext.getResource());

        fileReaderService.readCsv(
                readActionContext.importObjectName(),
                readActionContext.importId(),
                readActionContext.fileUrl(),
                objectConfig.getBufferSize(),
                readActionContext.rowConsumer()
        );
    }

//    @Handler(action = ObjectActionNamed.Csv.DETECT_COLUMN_HEADER_MAPPING)
//    public Map<Integer, String> columnMapping(ActionContext<ColumnHeaderMapperContext<Row>> actionContext) {
//        ColumnHeaderMapperContext<CSVRecord> context = actionContext.getPayload();
//        Map<Integer, String> dataHeaderMap = new LinkedHashMap<>();
//        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.objectName());
//        context.rowData().forEach(cellData -> {
//            Object value = ImportHelper.getCellValue(cellData);
//            String columnName = Utils.STR.trim(Utils.STR.valueOf(value));
//            String fieldName = Utils.STR.toCamelCaseUnderscore(columnName);
//            if (objectMetadata.containsAttribute(fieldName)) {
//                dataHeaderMap.put(cellData.getRowIndex(), fieldName);
//            }
//        });
//        return dataHeaderMap;
//    }

    @Handler(action = ObjectActionNamed.Csv.ROW_MAPPING)
    public Map<String, Object> rowMapping(ActionContext<RowMappingContext<CSVRecord>> actionContext) {
        return mapRow(actionContext.getPayload());
    }

    @Handler(action = ObjectActionNamed.Csv.CELL_MAPPING)
    public Object cellProcessMap(ActionContext<CellMappingContext> actionContext) {
        CellMappingContext cellMappingContext = actionContext.getPayload();
        Map<String, Object> rowData = cellMappingContext.rowData();
        Object attrVal = rowData.get(cellMappingContext.attribute().getFieldName());

        if (AttributeRefHelper.hasRef(cellMappingContext.attribute())) {
            return attrVal;
        }

        if (Objects.nonNull(attrVal) && cellMappingContext.attribute().isCollectionField()) {
            return ImportHelper.arrayParser(String.valueOf(attrVal));
        }

        return cellMappingContext.attribute().cast(attrVal);
    }


    @Handler(action = ObjectActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> codeRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return attributeReferenceResolver.resolve(attributeReferenceContext.importObjectName(), attributeReferenceContext.attribute(), attributeReferenceContext.data());
    }


    @Handler(action = ObjectActionNamed.Csv.BATCH_ROW_DATA_PROCESSING)
    public void batchRowDataProcessing(ActionContext<BatchRowData> actionContext) {
        BatchRowData batchRowData = actionContext.getPayload();
        ObjectMetadata metadata = ObjectMetadataFactory.getObjectMetadataByName(batchRowData.importObjectName());
        batchValidationService.processBatch(batchRowData.importId(), metadata, batchRowData.rowData());
    }






    public Map<String, Object> mapRow(RowMappingContext<CSVRecord> context) {
        Map<String, Object> dataRowMap = new LinkedHashMap<>();
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.objectName());
        objectMetadata.getAttributes().forEach(attribute -> {
            //Parsers
            String columnName = attribute.getColumnName();

            if(context.rowData().isMapped(columnName)
                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
                try {
                    Object columnData = context.rowData().get(columnName);
                    if (AttributeRefHelper.hasRef(attribute)) {
                        dataRowMap.put(attribute.getFieldName(), columnData);
                    } else {
//                        Object val;
//                        if (Objects.nonNull(columnData) && attribute.isCollectionField()) {
//                            val = Parsers.arrayParser(String.valueOf(columnData));
//                        } else {
//                            val = attribute.cast(columnData);
//                        }
//                        dataRowMap.put(attribute.getFieldName(), val);


//                        Object columnDataValue = cellMapping(context.objectName(), attribute, columnData);
//                        dataRowMap.put(attribute.getFieldName(), columnDataValue);

                        dataRowMap.put(attribute.getFieldName(), columnData);
                    }
                } catch (Exception e) {
                    log.error("Error while reading column data for attribute: {}, value: {}", columnName, context.rowData().get(columnName), e);
                }
            }
        });

        return dataRowMap;
    }

//    public Object cellMap(CellMapperContext mapperContext) {
//
//        Map<String, Object> rowData = mapperContext.rowData();
//        Object attrVal = rowData.get(mapperContext.attribute().getFieldName());
//
//        if (AttributeRefHelper.hasRef(mapperContext.attribute())) {
//            return attrVal;
//        }
//
//        if (Objects.nonNull(attrVal) && mapperContext.attribute().isCollectionField()) {
//            return ImportHelper.arrayParser(String.valueOf(attrVal));
//        }
//
//        return mapperContext.attribute().cast(attrVal);
//    }


//    public void readRow(String importObjectName, String fileUrl, Consumer<RawRow<CSVRecord>> consumer) {
//
//        CharsetDecoder decoder = StandardCharsets.UTF_8
//                .newDecoder()
//                .onMalformedInput(CodingErrorAction.IGNORE)
//                .onUnmappableCharacter(CodingErrorAction.IGNORE);
//
//        int bufferSize = config.getImportFile().bufferSize(importObjectName);
//
//        try (BOMInputStream bomInputStream = BOMInputStream.builder().setURI(URI.create(fileUrl)).get();
//             InputStreamReader isr = new InputStreamReader(bomInputStream, decoder);
//             BufferedReader reader = new BufferedReader(isr, bufferSize)
//        ) {
//            CSVFormat csvFormat = CsvDelimiterDetector.detect(
//                    reader,
//                    bufferSize
//            );
//
//            try (CSVParser csvParser = csvFormat.parse(reader)) {
//
//                for (CSVRecord csvRecord : csvParser) {
//                    consumer.accept(
//                            new RawRow<>(csvRecord.getRecordNumber(), importObjectName, csvRecord)
//                    );
//                }
//            }
//
//        } catch (Exception e) {
//            throw Lombok.sneakyThrow(e);
//        }
//    }



}

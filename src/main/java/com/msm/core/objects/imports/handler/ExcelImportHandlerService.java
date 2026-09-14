package com.msm.core.objects.imports.handler;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.imports.BatchProcessingService;
import com.msm.core.objects.imports.ImportActionNamed;
import com.msm.core.objects.imports.ImportHelper;
import com.msm.core.objects.imports.model.BatchRowData;
import com.msm.core.objects.imports.model.CellMapperContext;
import com.msm.core.objects.imports.model.ColumnHeaderMapperContext;
import com.msm.core.objects.imports.model.ReadActionContext;
import com.msm.core.objects.imports.model.RowMapperContext;
import com.msm.core.objects.imports.reference.AttributeRefHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class ExcelImportHandlerService {

    private final BatchProcessingService batchProcessingService;
    private final ActionExecutor actionExecutor;
    private final GenericObjectConfigProperties config;


    @Handler(action = ImportActionNamed.Excel.READ_FILE)
    public void read(ActionContext<ReadActionContext<Row>> actionContext) {
        ReadActionContext<Row> readActionContext = actionContext.getPayload();
        ImportHelper.readExcelStream(
                readActionContext.importObjectName(),
                readActionContext.fileUrl(),
                config.getImportFile().bufferSize(readActionContext.importObjectName()),
                config.getImportFile().batchSize(readActionContext.importObjectName()),
                readActionContext.rowConsumer()
        );
    }

    @Handler(action = ImportActionNamed.Excel.DETECT_COLUMN_HEADER_MAPPING)
    public Map<Integer, String> columnMapping(ActionContext<ColumnHeaderMapperContext<Row>> actionContext) {
        ColumnHeaderMapperContext<Row> context = actionContext.getPayload();
        Map<Integer, String> dataHeaderMap = new LinkedHashMap<>();
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.objectName());
        context.rowData().forEach(cellData -> {
            Object value = ImportHelper.getCellValue(cellData);
            String columnName = Utils.STR.trim(Utils.STR.valueOf(value));
            String fieldName = Utils.STR.toCamelCaseUnderscore(columnName);
            if (objectMetadata.containsAttribute(fieldName)) {
                dataHeaderMap.put(cellData.getColumnIndex(), fieldName);
            }
        });
        return dataHeaderMap;
    }

    @Handler(action = ImportActionNamed.Excel.ROW_MAPPING)
    public Map<String, Object> rowMapping(ActionContext<RowMapperContext<Row>> actionContext) {
        return mapRow(actionContext.getPayload());
    }

    @Handler(action = ImportActionNamed.Excel.CELL_MAPPING)
    public Object cellProcessMap(ActionContext<CellMapperContext> actionContext) {
        return cellMap(actionContext.getPayload());
    }

    @Handler(action = ImportActionNamed.Excel.BATCH_ROW_DATA_PROCESSING)
    public void batchRowDataProcessing(ActionContext<BatchRowData> actionContext) {
        BatchRowData batchRowData = actionContext.getPayload();
        ObjectMetadata metadata = ObjectMetadataFactory.getObjectMetadataByName(batchRowData.importObjectName());
        batchProcessingService.processBatch(batchRowData.importId(), metadata, batchRowData.rowData());
    }



    public Object cellMap(CellMapperContext mapperContext) {

        Map<String, Object> rowData = mapperContext.rowData();
        Object attrVal = rowData.get(mapperContext.attribute().getFieldName());

        if (AttributeRefHelper.hasRef(mapperContext.attribute())) {
            return attrVal;
        }

        if (Objects.nonNull(attrVal) && mapperContext.attribute().isCollectionField()) {
            return ImportHelper.arrayParser(String.valueOf(attrVal));
        }

        return mapperContext.attribute().cast(attrVal);
    }


    public Map<String, Object> mapRow(RowMapperContext<Row> context) {
        Map<String, Object> dataRowMap = new LinkedHashMap<>();
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.objectName());
        objectMetadata.getAttributes().forEach(attribute -> {
            //Parsers
            String columnName = attribute.getColumnName();
            context.rowData().forEach(cellData -> {
                log.warn("Cell value: {}", ImportHelper.getCellValue(cellData));
            });
        });

        return dataRowMap;
    }



//    public void readExcelStream(String importObjectName, String fileUrl, Consumer<RawRow<Row>> consumer) {
//        Workbook workbook = null;
//        InputStream inputStream = null;
//        try {
//            URL url = URI.create(fileUrl).toURL();
//            inputStream = url.openStream();
//            int bufferSize = config.getImportFile().bufferSize(importObjectName);
//            int batchSize = config.getImportFile().batchSize(importObjectName);
//            workbook = StreamingReader.builder()
//                    .rowCacheSize(batchSize)           // Data hold in RAM (Buffer Size)
//                    .bufferSize(bufferSize)            // Buffer size of InputStream (Bytes)
//                    .open(inputStream);
//
//            Sheet sheet = workbook.getSheetAt(0);
//            Iterator<Row> rowIterator = sheet.iterator();
//            if (rowIterator.hasNext()) {
//                rowIterator.next();
//            }
//
//            while (rowIterator.hasNext()) {
//                Row row = rowIterator.next();
//                consumer.accept(
//                        new RawRow<>(row.getRowNum(), importObjectName, row)
//                );
//            }
//        } catch (IOException e) {
//            throw Lombok.sneakyThrow(e);
//        } finally {
//            if (Objects.nonNull(workbook)) {
//                try {
//                    workbook.close();
//                } catch (IOException e) {
//                    log.error("Error while close workbook: {}", e.getMessage(), e);
//                }
//            }
//            if (Objects.nonNull(inputStream)) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    log.error("Error while close inputStream: {}", e.getMessage(), e);
//                }
//            }
//        }
//    }



}

package com.msm.core.objects.imports.excel.handler;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.imports.BatchImportService;
import com.msm.core.objects.imports.BatchImportServiceV2;
import com.msm.core.objects.imports.BatchValidationService;
import com.msm.core.objects.imports.FileReaderService;
import com.msm.core.objects.imports.ImportConfigService;
import com.msm.core.objects.imports.ImportHelper;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import com.msm.core.objects.imports.model.BatchInsertDataContext;
import com.msm.core.objects.imports.model.BatchInsertDataResult;
import com.msm.core.objects.imports.model.BatchRowData;
import com.msm.core.objects.imports.model.CellMappingContext;
import com.msm.core.objects.imports.model.ColumnToAttributeMappingContext;
import com.msm.core.objects.imports.model.ReadActionContext;
import com.msm.core.objects.imports.model.RowMappingContext;
import com.msm.core.objects.imports.reference.AttributeRefHelper;
import com.msm.core.objects.imports.reference.AttributeReferenceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class ExcelImportHandlerService {

    private final BatchValidationService batchValidationService;
    private final FileReaderService fileReaderService;
    private final AttributeReferenceResolver attributeReferenceResolver;
    private final ImportConfigService importConfigService;
    private final BatchImportService batchImportService;
    private final BatchImportServiceV2 batchImportServiceV2;

    @Handler(action = ObjectActionNamed.Excel.READ_FILE)
    public void read(ActionContext<ReadActionContext<Row>> actionContext) {
        ReadActionContext<Row> readActionContext = actionContext.getPayload();
        ObjectImportRegistry.ProcessingConfig objectConfig = importConfigService.getProcessingConfig(actionContext.getResource());

        fileReaderService.readExcelStream(
                readActionContext.importObjectName(),
                readActionContext.importId(),
                readActionContext.fileUrl(),
                objectConfig.getBufferSize(),
                objectConfig.getBatchSize(),
                readActionContext.rowConsumer()
        );
    }

    @Handler(action = ObjectActionNamed.Excel.DETECT_COLUMN_HEADER_MAPPING)
    public Map<Integer, String> attributeColumnHeaderMapping(ActionContext<ColumnToAttributeMappingContext<Row>> actionContext) {
        ColumnToAttributeMappingContext<Row> context = actionContext.getPayload();
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

    @Handler(action = ObjectActionNamed.Excel.ROW_MAPPING)
    public Map<String, Object> rowMapping(ActionContext<RowMappingContext<Row>> actionContext) {
        RowMappingContext<Row> context = actionContext.getPayload();
        Map<String, Object> dataRowMap = new LinkedHashMap<>();
        context.rowData().forEach(cellData -> {
            Object data = ImportHelper.getCellValue(cellData);
            String fieldName = context.headerColumn().get(cellData.getColumnIndex());
            if(fieldName != null) {
                dataRowMap.put(fieldName, data);
            }
        });

        return dataRowMap;
    }

    @Handler(action = ObjectActionNamed.Excel.CELL_MAPPING)
    public Object cellProcessMap(ActionContext<CellMappingContext> actionContext) {
        CellMappingContext mapperContext =  actionContext.getPayload();
        Map<String, Object> rowData = mapperContext.rowData();
        Object attrVal = rowData.get(mapperContext.attribute().getFieldName());

        if (AttributeRefHelper.hasRef(mapperContext.attribute())) {
            return attrVal;
        }

        if (Objects.nonNull(attrVal) && mapperContext.attribute().isCollectionField()) {
            return ImportHelper.arrayParser(String.valueOf(attrVal));
        }

        if (mapperContext.attribute().getJavaType().isTypeOrSubTypeOf(Instant.class)) {
            return Utils.DATES.toInstant(Utils.STR.valueOf(attrVal));
        }

        return mapperContext.attribute().cast(attrVal);
    }


    @Handler(action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> codeRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return attributeReferenceResolver.resolve(attributeReferenceContext.importObjectName(), attributeReferenceContext.attribute(), attributeReferenceContext.data());
    }

    @Handler(action = ObjectActionNamed.Excel.DATA_VALIDATE_PROCESSING)
    public void batchRowDataProcessing(ActionContext<BatchRowData> actionContext) {
        BatchRowData batchRowData = actionContext.getPayload();
        ObjectMetadata metadata = ObjectMetadataFactory.getObjectMetadataByName(batchRowData.importObjectName());
        batchValidationService.processBatch(batchRowData.importId(), metadata, batchRowData.rowData());
    }

    @Handler(action = ObjectActionNamed.Excel.BATCH_INSERT_DATA_PROCESSING)
    public BatchInsertDataResult batchInsertDataProcessing(ActionContext<BatchInsertDataContext> actionContext) {
        BatchInsertDataContext batchInsertDataContext = actionContext.getPayload();
        return batchImportServiceV2.batchInsertDataProcessing(batchInsertDataContext.jobId(), batchInsertDataContext.objectName(), batchInsertDataContext.data());
    }


//    @CellMappingHandler(resource = "profile.fromDate")
//    public Object createdAt(ActionContext<CellMappingContext> actionContext) {
//        CellMappingContext cellMapperContext = actionContext.getPayload();
//        Map<String, Object> rowData = cellMapperContext.rowData();
//        Object attrVal = rowData.get(cellMapperContext.attribute().getFieldName());
//        Object object = Utils.DATES.toInstant(Utils.STR.valueOf(attrVal));
//        System.out.println(object);
//        return object;
//    }
//
//    @CellMappingHandler(resource = "profile.toDate")
//    public Object toDate(ActionContext<CellMappingContext> actionContext) {
//        CellMappingContext cellMapperContext = actionContext.getPayload();
//        Map<String, Object> rowData = cellMapperContext.rowData();
//        Object attrVal = rowData.get(cellMapperContext.attribute().getFieldName());
//        Object object = Utils.DATES.toInstant(Utils.STR.valueOf(attrVal));
//        System.out.println(object);
//        return object;
//    }

}

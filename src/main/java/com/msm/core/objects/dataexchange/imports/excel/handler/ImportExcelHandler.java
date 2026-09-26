package com.msm.core.objects.dataexchange.imports.excel.handler;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.ColumnToAttributeMappingContext;
import com.msm.core.objects.dataexchange.DataHelper;
import com.msm.core.objects.dataexchange.imports.ImportConfigService;
import com.msm.core.objects.dataexchange.imports.ImportDataService;
import com.msm.core.objects.dataexchange.imports.ImportValidationService;
import com.msm.core.objects.dataexchange.imports.csv.FileReaderService;
import com.msm.core.objects.dataexchange.imports.model.AttributeReferenceContext;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataContext;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataResult;
import com.msm.core.objects.dataexchange.imports.model.BatchRowData;
import com.msm.core.objects.dataexchange.imports.model.CellMappingContext;
import com.msm.core.objects.dataexchange.imports.model.ReadActionContext;
import com.msm.core.objects.dataexchange.imports.model.RowMappingContext;
import com.msm.core.objects.dataexchange.imports.reference.AttributeRefHelper;
import com.msm.core.objects.dataexchange.imports.reference.AttributeReferenceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class ImportExcelHandler {

    private final ImportValidationService importValidationService;
    private final FileReaderService fileReaderService;
    private final AttributeReferenceResolver attributeReferenceResolver;
    private final ImportConfigService importConfigService;
    private final ImportDataService importDataService;

    @Handler(action = ObjectActionNamed.Excel.READ_FILE)
    public void readExcel(ActionContext<ReadActionContext<Row>> actionContext) {
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
            Object value = DataHelper.getCellValue(cellData);
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
            Object data = DataHelper.getCellValue(cellData);
            String fieldName = context.headerColumn().get(cellData.getColumnIndex());
            if(fieldName != null) {
                dataRowMap.put(fieldName, data);
            }
        });

        return dataRowMap;
    }

    @Handler(action = ObjectActionNamed.Excel.CELL_MAPPING)
    public Object cellMapping(ActionContext<CellMappingContext> actionContext) {
        CellMappingContext mapperContext =  actionContext.getPayload();
        Map<String, Object> rowData = mapperContext.rowData();
        Object attrVal = rowData.get(mapperContext.attribute().getFieldName());

        if (AttributeRefHelper.hasRef(mapperContext.attribute())) {
            return attrVal;
        }

        if (Objects.nonNull(attrVal) && mapperContext.attribute().isCollectionField()) {
            return DataHelper.arrayParser(String.valueOf(attrVal));
        }

        if (mapperContext.attribute().getJavaType().isTypeOrSubTypeOf(Instant.class)) {
            if(attrVal instanceof Date) {
                return Utils.DATES.toInstant((Date) attrVal);
            }
            return Utils.DATES.toInstant(Utils.STR.valueOf(attrVal));
        }

        if (mapperContext.attribute().getJavaType().isTypeOrSubTypeOf(LocalDate.class)) {
            if(attrVal instanceof Date) {
                return Utils.DATES.toLocalDate((Date) attrVal);
            }
            return Utils.DATES.toLocalDate(Utils.STR.valueOf(attrVal));
        }

        return mapperContext.attribute().cast(attrVal);
    }


    @Handler(action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> refMapping(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return attributeReferenceResolver.resolve(attributeReferenceContext.importObjectName(), attributeReferenceContext.attribute(), attributeReferenceContext.data());
    }

    @Handler(action = ObjectActionNamed.Excel.DATA_VALIDATE_PROCESSING)
    public void validateData(ActionContext<BatchRowData> actionContext) {
        BatchRowData batchRowData = actionContext.getPayload();
        ObjectMetadata metadata = ObjectMetadataFactory.getObjectMetadataByName(batchRowData.importObjectName());
        importValidationService.processValidate(batchRowData.importId(), metadata, batchRowData.rowData());
    }

    @Handler(action = ObjectActionNamed.Excel.BATCH_INSERT_OR_UPDATE_DATA_PROCESSING)
    public BatchInsertDataResult batchInsertDataProcessing(ActionContext<BatchInsertDataContext> actionContext) {
        BatchInsertDataContext batchInsertDataContext = actionContext.getPayload();
        return importDataService.batchInsertDataProcessing(batchInsertDataContext.jobId(), batchInsertDataContext.objectName(), batchInsertDataContext.data());
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

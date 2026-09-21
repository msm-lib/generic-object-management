package com.msm.core.objects.imports.excel.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.imports.ImportHelper;
import com.msm.core.objects.imports.model.ColumnToAttributeMappingContext;
import com.msm.core.objects.imports.model.RowMappingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class ExportExcelHandlerService {
    private static final String CODE = "code";
//    private final ObjectQueryRepository internalObjectQueryRepository;
//    private final S3Client s3Client;

    @Handler(action = ObjectActionNamed.Excel.Export.CREATE_HEADER)
    public List<Row> exportExcelData(ActionContext<Map<String, Object>> actionContext) {

        DataRecord exportJobRecord = DataRecord.ofNullable(actionContext.getPayload());

        List<String> headers = new ArrayList<>();
//        try (InputStream s3InputStream = s3Client.getObject(GetObjectRequest.builder()
//                .bucket(BUCKET_NAME)
//                .key("template/export_template.xlsx")
//                .build())) {
//
//            Workbook templateWorkbook = new XSSFWorkbook(s3InputStream);
//            Sheet templateSheet = templateWorkbook.getSheetAt(0);
//            Row headerRow = templateSheet.getRow(0);
//            for (Cell cell : headerRow) {
//                headers.add(cell.getStringCellValue());
//            }
//            templateSheet.createRow(headers.size());
//            templateWorkbook.close();
//        }


        return null;
    }


    @Handler(action = ObjectActionNamed.Excel.Export.DETECT_COLUMN_HEADER_MAPPING)
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

    @Handler(action = ObjectActionNamed.Excel.Export.ROW_MAPPING)
    public Map<String, Object> rowMapping(ActionContext<RowMappingContext<Map<String, Object>>> actionContext) {
        RowMappingContext<Map<String, Object>> context = actionContext.getPayload();
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(context.objectName());
        context.rowData().keySet().forEach(dataKey -> {
            Attribute attribute = objectMetadata.getAttributeByName(dataKey);
            if (Objects.nonNull(attribute) && attribute.hasRef()) {
                Map<String, Object> refMap = DataRecord
                        .ofNullable(context.rowData())
                        .get(
                                attribute.getAttributeRef().getFieldName(),
                                new TypeReference<>() {}
                        );
                if(Utils.CL.isNotEmpty(refMap)) {
                    context.rowData().put(dataKey, refMap.get(CODE));
                }
            }
        });

        return context.rowData();
    }
}

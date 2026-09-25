package com.msm.core.objects.dataexchange.imports.excel;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.dataexchange.ColumnToAttributeMappingContext;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataContext;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataResult;
import com.msm.core.objects.dataexchange.imports.model.BatchRowData;
import com.msm.core.objects.dataexchange.imports.model.CellMappingContext;
import com.msm.core.objects.dataexchange.imports.model.ImportRow;
import com.msm.core.objects.dataexchange.imports.model.RawRow;
import com.msm.core.objects.dataexchange.imports.model.RowMappingContext;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@RequiredArgsConstructor
public class ImportDataExecutor {
    private static final Set<String> IGNORE_ATTRIBUTE = Set.of("customValues");
    private final ActionExecutor actionExecutor;

    public Map<Integer, String> detectColumnHeaderMapping(UUID importId, RawRow<Row> row) {

        ColumnToAttributeMappingContext<Row> mapperContext = ColumnToAttributeMappingContext.of(
                importId,
                row.rowNumber(),
                row.objectName(),
                row.data()
        );
        ActionContext<ColumnToAttributeMappingContext<Row>> actionContext = ActionContext
                .<ColumnToAttributeMappingContext<Row>>builder()
                .resource(row.objectName())
                .action(ObjectActionNamed.Excel.DETECT_COLUMN_HEADER_MAPPING)
                .payload(mapperContext)
                .build();

        Map<Integer, String> columnHeaderMap = actionExecutor.execute(actionContext);
        if(Utils.CL.isEmpty(columnHeaderMap)) {
            return null;
        }
        return columnHeaderMap;
    }

    public Map<String, Object> rowMapping(UUID importId, Map<Integer, String> headerColumn, RawRow<Row> row) {

        RowMappingContext<Row> mapperContext = RowMappingContext.of(
                importId,
                row.rowNumber(),
                row.objectName(),
                headerColumn,
                row.data()
        );
        ActionContext<RowMappingContext<Row>> actionContext = ActionContext
                .<RowMappingContext<Row>>builder()
                .resource(row.objectName())
                .action(ObjectActionNamed.Excel.ROW_MAPPING)
                .payload(mapperContext)
                .build();

        return actionExecutor.execute(actionContext);
    }

    public void cellMapping(
            UUID importId,
            String objectName,
            Map<Integer, String> headerColumn,
            Map<String, Object> rowData
    ) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attribute -> {
            if (rowData.containsKey(attribute.getFieldName())
                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
                String objectCellResource = Utils.STR.format("{0}.{1}",  objectName, attribute.getFieldName());
                CellMappingContext cellMappingContext = CellMappingContext.of(importId, objectName, attribute, headerColumn, rowData);
                ActionContext<CellMappingContext> actionContext = ActionContext
                        .<CellMappingContext>builder()
                        .resource(objectCellResource)
                        .action(ObjectActionNamed.Excel.CELL_MAPPING)
                        .payload(cellMappingContext)
                        .build();

                Object columnDataValue = actionExecutor.execute(actionContext);;
                rowData.put(attribute.getFieldName(), columnDataValue);
            }
        });
    }

    public void batchDataValidateAction(UUID importId, String importObjectName, List<ImportRow> rows) {
        ActionContext<BatchRowData> actionContext = ActionContext
                .<BatchRowData>builder()
                .resource(importObjectName)
                .action(ObjectActionNamed.Excel.DATA_VALIDATE_PROCESSING)
                .payload(BatchRowData.of(importId, importObjectName, rows))
                .build();
        actionExecutor.execute(actionContext);
    }


    public BatchInsertDataResult batchInsertDataAction(UUID importId, String importObjectName, List<Map<String, Object>> data) {
        ActionContext<BatchInsertDataContext> actionContext = ActionContext
                .<BatchInsertDataContext>builder()
                .resource(importObjectName)
                .action(ObjectActionNamed.Excel.BATCH_INSERT_OR_UPDATE_DATA_PROCESSING)
                .payload(BatchInsertDataContext.of(importId, importObjectName, data))
                .build();
        return actionExecutor.execute(actionContext);
    }


    public Map<String, Object> updateObject(String objectName, Object id, Map<String, Object> row) {
        ActionContext<Map<String, Object>> actionContext = ActionContext
                .<Map<String, Object>>builder()
                .resource(objectName)
                .objectId(id)
                .action(Constants.Action.UPDATE)
                .payload(row)
                .build();

        return actionExecutor.execute(actionContext);
    }
}

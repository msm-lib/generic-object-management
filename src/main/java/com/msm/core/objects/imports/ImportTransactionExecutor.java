package com.msm.core.objects.imports;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ImportTransactionExecutor {

    private final ActionExecutor actionExecutor;


    public List<Map<String, Object>> bulkUpdate(String objectName, List<Map<String, Object>> row) {
        ActionContext<List<Map<String, Object>>> actionContext = ActionContext
                .<List<Map<String, Object>>>builder()
                .resource(objectName)
                .action(Constants.Action.BULK_UPDATE)
                .payload(row)
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

    public List<Map<String, Object>> bulkInsert(String objectName, List<Map<String, Object>> row) {
        ActionContext<List<Map<String, Object>>> actionContext = ActionContext
                .<List<Map<String, Object>>>builder()
                .resource(objectName)
                .action(Constants.Action.BULK_CREATE)
                .payload(row)
                .build();

        return actionExecutor.execute(actionContext);
    }

    public Map<String, Object> insertObject(String objectName, Map<String, Object> row) {
        ActionContext<Map<String, Object>> actionContext = ActionContext
                .<Map<String, Object>>builder()
                .resource(objectName)
                .action(Constants.Action.CREATE)
                .payload(row)
                .build();

        return actionExecutor.execute(actionContext);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBatch(
            String objectName,
            List<DataRecord> records
    ) {
//        repository.update(
//                objectName,
//                records.stream()
//                        .map(r -> r.get(ImportStagingMeta.DATA))
//                        .collect(Collectors.toList())
//        );

        bulkUpdate(
                objectName,
                records.stream()
                        .map(r -> r.get(ImportStagingMeta.DATA))
                        .collect(Collectors.toList())
        );
    }



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSingle(
            String objectName,
            DataRecord record
    ) {
//        repository.update(
//                objectName,
//                List.of(record.get(ImportStagingMeta.DATA))
//        );
        Map<String, Object> row = record.get(ImportStagingMeta.DATA);
        String primaryAttrName = ObjectMetadataFactory.getObjectMetadataByName(objectName).getIdAttribute().getFieldName();
        updateObject(
                objectName,
                row.get(primaryAttrName),
                row
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertBatch(
            String objectName,
            List<DataRecord> records
    ) {
//        repository.insertBatch(
//                objectName,
//                records.stream()
//                        .map(r -> r.get(ImportStagingMeta.DATA))
//                        .collect(Collectors.toList())
//        );

        bulkInsert(
                objectName,
                records.stream()
                        .map(r -> r.get(ImportStagingMeta.DATA))
                        .collect(Collectors.toList())
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertSingle(
            String objectName,
            DataRecord record
    ) {
//        repository.insertBatch(
//                objectName,
//                List.of(record.get(ImportStagingMeta.DATA))
//        );

        insertObject(
                objectName,
                record.get(ImportStagingMeta.DATA)
        );
    }
}

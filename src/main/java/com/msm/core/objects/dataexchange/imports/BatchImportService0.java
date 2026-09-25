package com.msm.core.objects.dataexchange.imports;

import com.msm.core.commons.Constants;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.excel.ImportDataExecutor;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataResult;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.InsertDataResult;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Deprecated
@Slf4j
@RequiredArgsConstructor
public class BatchImportService0 {

    private final ImportErrorService importErrorService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ImportConfigService importConfigService;
    private final ImportDataExecutor importDataExecutor;


    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchInsertDataResult batchInsertDataProcessing(
            UUID importId,
            String importObjectName,
            List<Map<String, Object>> rows
    ) {
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(importObjectName);
        List<DataRecord> errors = new ArrayList<>();
        List<InsertDataResult> insertDataResults = new ArrayList<>();
        long success = 0;

        for (Map<String, Object> row : rows) {
            DataRecord importStagingRecord = DataRecord.of(row);

            try {
                ObjectImportRegistry.IdentityConfig identityConfig = importConfigService.getObject(importObjectName).getIdentity();
                ObjectImportRegistry.StrategyMode mode =  identityConfig.getStrategy();
                Object id = getId(row);
                if(Objects.nonNull(id)) {
//                    update(importObjectName, id, row);
                    importDataExecutor.updateObject(importObjectName, id, importStagingRecord.get(ImportStagingMeta.DATA));
                } else {
                    if(ObjectImportRegistry.StrategyMode.UPSERT.equals(mode)) {
                        Condition condition = Objects.nonNull(identityConfig.getCondition()) ? DSL.condition(identityConfig.getCondition()) : DSL.noCondition();
                        upsert(importObjectName, importStagingRecord.get(ImportStagingMeta.DATA), identityConfig.getFields(), condition);
                    } else {
                        Condition condition = DSL.noCondition();
                        Attribute primaryAttr = objectMetadata.getIdAttribute();
                        for (String attrName : identityConfig.getFields()) {
                            Attribute attribute = objectMetadata.getAttributeByName(attrName);
                            Field<Object> field = (Field<Object>) attribute.getField();
                            Object val = row.get(attrName);
                            if(Objects.nonNull(val)) {
                                condition = condition.and(field.eq(val));
                            }
                        }

                        Map<String, Object> objectMap = internalObjectQueryRepository.findOneByCondition(
                                importObjectName,
                                condition,
                                List.of(primaryAttr.getFieldName())
                        );

                        if(Objects.nonNull(objectMap)) {
                            Object objectIdExists = objectMap.get(primaryAttr.getFieldName());
                            row.put(primaryAttr.getFieldName(), objectIdExists);
                            update(importObjectName, objectIdExists, row);
                        }
                    }
                }

//                importOne(importObjectName, importStagingRecord.get(ImportStagingMeta.DATA));
                success++;
                insertDataResults.add(
                        new InsertDataResult(
                                success,
                                ImportStatus.COMPLETED,
                                "COMPLETED",
                                row
                        )
                );
            } catch (Exception e) {
                errors.add(DataRecord.of()
                        .with(ImportErrorMeta.IMPORT_ID, importId)
                        .with(ImportErrorMeta.ROW_NUMBER, importStagingRecord.get(ImportStagingMeta.ROW_NUMBER))
                        .with(ImportErrorMeta.ERROR_TYPE, "DATABASE")
                        .with(ImportErrorMeta.ERROR_MESSAGE, e.getMessage())
                        .with(ImportErrorMeta.ERROR_CODE, "IMPORT_ERROR")
                        .with(ImportStagingMeta.DATA, row)
                );
                insertDataResults.add(
                        new InsertDataResult(
                                success,
                                ImportStatus.COMPLETED_WITH_ERRORS,
                                "COMPLETED_WITH_ERRORS",
                                row
                        )
                );
            }
        }

        if (!errors.isEmpty()) {
            importErrorService.insertErrors(errors);
        }

        return new BatchInsertDataResult(
                success,
                errors.size(),
                insertDataResults
        );
    }


    private void upsert(
            String objectName,
            Map<String, Object> row,
            List<String> conflictFields,
            Condition condition
    ) {
        internalObjectQueryRepository.upsert(
                objectName,
                row,
                conflictFields,
                condition
        );
    }

    private void update(
            String objectName,
            Object id,
            Map<String, Object> row
    ) {
        internalObjectQueryRepository.update(
                objectName,
                id,
                row
        );
    }

    private void importOne(
            String objectName,
            Map<String, Object> row
    ) {
        try {
            internalObjectQueryRepository.save(objectName, row);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Duplicate detected for object: {}, falling back to update. Error: {}", objectName, e.getMessage());
        }
    }

    private Object getId(Map<String, Object> data) {
        return DataRecord.of(data).get(ImportStagingMeta.DATA).get(Constants.OBJECT_PK);
    }

    private Object extractValues(Map<String, Object> data) {
        return DataRecord.of(data).get(ImportStagingMeta.DATA).get(Constants.OBJECT_PK);
    }
}

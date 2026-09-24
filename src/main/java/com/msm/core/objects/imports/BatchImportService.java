package com.msm.core.objects.imports;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.imports.excel.ImportDataExecutor;
import com.msm.core.objects.imports.model.BatchInsertDataResult;
import com.msm.core.objects.imports.model.ImportStatus;
import com.msm.core.objects.imports.model.InsertDataResult;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.RowN;
import org.jooq.impl.DSL;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class BatchImportService {

    private final ImportErrorService importErrorService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ImportConfigService importConfigService;
    private final ImportDataExecutor importDataExecutor;


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

        ObjectImportRegistry.ObjectConfig objectConfig = importConfigService.getObject(objectMetadata.getName());

        List<Map<String, Object>> data = findData(objectMetadata, objectConfig.getIdentity().getFields(), rows);
        Map<String, Map<String, Object>> existingByIdentity = Utils.D.groupBy(
                data,
                objectMap -> {
                    return IdentityKeyGenerator.generate(objectMap, objectConfig.getIdentity().getFields()).key();
                },
                Function.identity(),
                (old, newData) -> old
        );

        List<DataRecord> updatedData = new ArrayList<>();
        List<DataRecord> insertData = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            DataRecord importStagingRecord = DataRecord.of(row);

            String identityKeyStaging = importStagingRecord.get(ImportStagingMeta.IDENTITY_KEY);
            Map<String, Object> duplicateDataRow = existingByIdentity.get(identityKeyStaging);
            if(duplicateDataRow != null) {
                DataRecord
                        .of(importStagingRecord.get(ImportStagingMeta.DATA))
                        .with(ImportStagingMeta.ID, DataRecord.of(duplicateDataRow).get(ImportStagingMeta.ID));
                updatedData.add(importStagingRecord);
            } else {
                insertData.add(importStagingRecord);
            }

            success++;
        }

        if(!updatedData.isEmpty()) {
            try {
                internalObjectQueryRepository.update(
                        objectMetadata.getName(),
                        updatedData.stream().map(dataRecord -> dataRecord.get(ImportStagingMeta.DATA)).collect(Collectors.toList())
                );
            } catch(Exception e) {
                updatedData.forEach(importStagingRecord -> {
                    errors.add(DataRecord.of()
                            .with(ImportErrorMeta.IMPORT_ID, importId)
                            .with(ImportErrorMeta.ROW_NUMBER, importStagingRecord.get(ImportStagingMeta.ROW_NUMBER))
                            .with(ImportErrorMeta.ERROR_TYPE, "DATABASE")
                            .with(ImportErrorMeta.ERROR_MESSAGE, e.getMessage())
                            .with(ImportErrorMeta.ERROR_CODE, "IMPORT_ERROR")
                            .with(ImportStagingMeta.DATA, importStagingRecord.get(ImportStagingMeta.DATA))
                    );
                    insertDataResults.add(
                            new InsertDataResult(
                                    updatedData.size(),
                                    ImportStatus.COMPLETED_WITH_ERRORS,
                                    "COMPLETED_WITH_ERRORS",
                                    importStagingRecord.getValues()
                            )
                    );
                });

            }
        }

        if(!insertData.isEmpty()) {
            try {
                internalObjectQueryRepository.insertBatch(
                        objectMetadata.getName(),
                        insertData.stream().map(dataRecord -> dataRecord.get(ImportStagingMeta.DATA)).collect(Collectors.toList())
                );
            } catch (Exception e) {
                insertData.forEach(importStagingRecord -> {
                    errors.add(DataRecord.of()
                            .with(ImportErrorMeta.IMPORT_ID, importId)
                            .with(ImportErrorMeta.ROW_NUMBER, importStagingRecord.get(ImportStagingMeta.ROW_NUMBER))
                            .with(ImportErrorMeta.ERROR_TYPE, "DATABASE")
                            .with(ImportErrorMeta.ERROR_MESSAGE, e.getMessage())
                            .with(ImportErrorMeta.ERROR_CODE, "IMPORT_ERROR")
                            .with(ImportStagingMeta.DATA, importStagingRecord.get(ImportStagingMeta.DATA))
                    );
                    insertDataResults.add(
                            new InsertDataResult(
                                    insertData.size(),
                                    ImportStatus.COMPLETED_WITH_ERRORS,
                                    "COMPLETED_WITH_ERRORS",
                                    importStagingRecord.getValues()
                            )
                    );
                });
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

    private List<Map<String, Object>> findData(ObjectMetadata objectMetadata, List<String> identityFields, List<Map<String, Object>> rows) {

        return internalObjectQueryRepository.findByCondition(
                objectMetadata.getName(),
                buildIdentityCondition(objectMetadata, identityFields, rows)
        );
    }


    public Condition buildIdentityCondition(ObjectMetadata objectMetadata, List<String> identityFields, List<Map<String, Object>> rows) {
        if (identityFields.isEmpty() || rows.isEmpty()) {
            return DSL.falseCondition();
        }

        Field<?>[] tableFields = identityFields.stream()
                .map(fieldName -> {
                   Attribute attribute = objectMetadata.getAttributeByName(fieldName);
                    return attribute.getField();
                })
                .toArray(Field[]::new);

        RowN tableRow = DSL.row(tableFields);

        List<RowN> values = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            DataRecord dataRecord = DataRecord.of(row);
            Map<String, Object> data = dataRecord.get(ImportStagingMeta.DATA);
            Field<?>[] rowValues = identityFields.stream()
                    .map(fieldName ->
                            DSL.val(data.get(fieldName))
                    )
                    .toArray(Field[]::new);

            values.add(DSL.row(rowValues));
        }

        return tableRow.in(values);
    }

}

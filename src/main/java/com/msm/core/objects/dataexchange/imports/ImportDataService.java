package com.msm.core.objects.dataexchange.imports;

import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.keys.IdentityKeyGenerator;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataResult;
import com.msm.core.objects.dataexchange.imports.model.InsertDataResult;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.RowN;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class ImportDataService {

    private final ImportErrorService importErrorService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ImportConfigService importConfigService;
    private final ImportDataProcessor importDataProcessor;


//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchInsertDataResult batchInsertDataProcessing(
            UUID importId,
            String importObjectName,
            List<Map<String, Object>> rows
    ) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(importObjectName);
        ObjectImportRegistry.ObjectConfig objectConfig = importConfigService.getObject(objectMetadata.getName());
        Attribute versionAttr = objectMetadata.getVersionAttribute();

        List<DataRecord> errors = new ArrayList<>();
        List<InsertDataResult> results = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return new BatchInsertDataResult(0, 0, results);
        }


        List<Map<String, Object>> existingData;

        try {
            existingData = findExistingData(
                    objectMetadata,
                    objectConfig.getIdentity().getFields(),
                    rows
            );
        } catch (Exception e) {
            String message = importDataProcessor.resolveErrorMessage(e);
            for (Map<String, Object> row : rows) {
                DataRecord stagingRecord = DataRecord.of(row);
                errors.add(importDataProcessor.buildError(
                        importId,
                        stagingRecord,
                        "DATABASE",
                        "IMPORT_FIND_DATA_ERROR",
                        message
                ));

                results.add(importDataProcessor.buildErrorResult(stagingRecord, message));
            }

            importErrorService.insertErrors(errors);

            return new BatchInsertDataResult(
                    0,
                    errors.size(),
                    results
            );
        }

        Map<String, Map<String, Object>> existingByIdentity = Utils.D.groupBy(
                existingData,
                objectMap -> IdentityKeyGenerator.generate(objectMap, objectConfig.getIdentity().getFields()).key(),
                Function.identity(),
                (old, newData) -> old
        );

        List<DataRecord> updateData = new ArrayList<>();
        List<DataRecord> insertData = new ArrayList<>();


        for (Map<String, Object> row : rows) {
            DataRecord stagingRecord = DataRecord.of(row);

            try {
                String identityKey = stagingRecord.get(ImportStagingMeta.IDENTITY_KEY);
                if (identityKey == null || identityKey.isBlank()) {
                    errors.add(importDataProcessor.buildError(
                            importId,
                            stagingRecord,
                            "VALIDATION",
                            "MISSING_IDENTITY",
                            "Identity key is missing."
                    ));

                    results.add(importDataProcessor.buildErrorResult(
                            stagingRecord,
                            "Identity key is missing."
                    ));

                    continue;
                }

                Map<String, Object> existingRow = existingByIdentity.get(identityKey);
                if (existingRow != null) {
                    DataRecord existingRecord = DataRecord.of(existingRow);
                    UUID existingId = existingRecord.get(ImportStagingMeta.ID);

                    DataRecord dataRecord = DataRecord
                            .of(stagingRecord.get(ImportStagingMeta.DATA))
                            .with(ImportStagingMeta.ID, existingId);

                    if(versionAttr != null) {
                        dataRecord.with(versionAttr.getFieldName(), existingRecord.getOrDefault(versionAttr.getFieldName(), Long.class, 0L));
                    }

                    stagingRecord.with(ImportStagingMeta.DATA, dataRecord.getValues());
                    updateData.add(stagingRecord);

                } else {
                    insertData.add(stagingRecord);
                }

            } catch (Exception e) {
                String message = importDataProcessor.resolveErrorMessage(e);
                errors.add(importDataProcessor.buildError(
                        importId,
                        stagingRecord,
                        "VALIDATION",
                        "INVALID_DATA",
                        message
                ));
                results.add(importDataProcessor.buildErrorResult(
                        stagingRecord,
                        message
                ));
            }
        }

        /*
         * ---------------------------------------------------------
         * 3. UPDATE
         * ---------------------------------------------------------
         */
        int successfulUpdate = importDataProcessor.processUpdateBatch(
                importId,
                objectMetadata.getName(),
                updateData,
                errors,
                results
        );

        /*
         * ---------------------------------------------------------
         * 4. INSERT
         * ---------------------------------------------------------
         */
        int successfulInsert = importDataProcessor.processInsertBatch(
                importId,
                objectMetadata.getName(),
                insertData,
                errors,
                results
        );

        /*
         * ---------------------------------------------------------
         * 5. Persist errors
         * ---------------------------------------------------------
         */
        if (!errors.isEmpty()) {
            importErrorService.insertErrors(errors);
        }

        return new BatchInsertDataResult(
                successfulUpdate + successfulInsert,
                errors.size(),
                results
        );
    }


    private List<Map<String, Object>> findExistingData(ObjectMetadata objectMetadata, List<String> identityFields, List<Map<String, Object>> rows) {

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

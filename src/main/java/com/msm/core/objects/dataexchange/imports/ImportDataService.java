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
        Attribute idAttr = objectMetadata.getIdAttribute();

        List<DataRecord> errors = new ArrayList<>();
        List<InsertDataResult> results = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return new BatchInsertDataResult(0, 0, results);
        }

        List<DataRecord> updateData = new ArrayList<>();
        List<DataRecord> insertData = new ArrayList<>();





        List<List<Map<String, Object>>> identityDataList = extractIdentityData(objectMetadata, rows);
        List<Map<String, Object>> statingDataExistingIds = identityDataList.getFirst();

        if (!statingDataExistingIds.isEmpty()) {
            applyStatingDataExistingIds(importObjectName, idAttr, versionAttr, updateData, statingDataExistingIds);
        }

        List<Map<String, Object>> statingData = identityDataList.getLast();

        if(!statingData.isEmpty()) {
            List<Map<String, Object>> existingData;
            try {
                existingData = findExistingData(
                        objectMetadata,
                        objectConfig.getIdentity().getFields(),
                        statingData
                );
            } catch (Exception e) {
                String message = importDataProcessor.resolveErrorMessage(e);
                for (Map<String, Object> row : statingData) {
                    DataRecord stagingRecord = DataRecord.of(row);
                    errors.add(importDataProcessor.buildError(
                            importId,
                            stagingRecord,
                            "VALIDATION",
                            "FIND_DATA_ERROR",
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


            for (Map<String, Object> row : statingData) {
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
            Field<?>[] rowValues = identityFields
                    .stream()
                    .map(fieldName -> {
                        Attribute attribute = objectMetadata.getAttributeByName(fieldName);
                        Object casted = attribute.cast(data.get(fieldName));
                        return DSL.val(casted);
                    })
                    .toArray(Field[]::new);

            values.add(DSL.row(rowValues));
        }

        return tableRow.in(values);
    }


    public List<List<Map<String, Object>>> extractIdentityData(ObjectMetadata objectMetadata, List<Map<String, Object>> rows) {
        Attribute idAttribute =  objectMetadata.getIdAttribute();
        List<Map<String, Object>> left = new  ArrayList<>();
        List<Map<String, Object>> right = new  ArrayList<>();
        rows.forEach(stagingDataMap -> {
            DataRecord stagingRecord = DataRecord.of(stagingDataMap);
            Map<String, Object> objectDataMap = stagingRecord.get(ImportStagingMeta.DATA);
            if(objectDataMap.containsKey(idAttribute.getFieldName())) {
                left.add(stagingDataMap);
            } else {
                right.add(stagingDataMap);
            }
        });

        return List.of(left, right);
    }


    public void applyStatingDataExistingIds(
            String importObjectName,
            Attribute idAttribute,
            Attribute versionAttribute,
            List<DataRecord> updateData,
            List<Map<String, Object>> statingDataExistingIds
    ) {

        if(versionAttribute == null) {
            statingDataExistingIds.forEach(data -> {
                updateData.add(DataRecord.of(data));
            });
            return;
        }

        //Find data id, version fron db
        List<Map<String, Object>> existingFromDbMap = internalObjectQueryRepository.findByIds(
                importObjectName,
                Utils.D.toList(
                        statingDataExistingIds,
                        statingData -> DataRecord.of(statingData).get(ImportStagingMeta.DATA).get(idAttribute.getFieldName())
                ),
                List.of(idAttribute.getFieldName(), versionAttribute.getFieldName())
        );

        //Group data by id
        Map<UUID, Map<String, Object>> existingFromDbByIdentity = Utils.D.groupBy(
                existingFromDbMap,
                objectMap -> (UUID) objectMap.get(idAttribute.getFieldName()),
                Function.identity(),
                (old, newData) -> old
        );

        statingDataExistingIds.forEach(row -> {
            DataRecord stagingRecord = DataRecord.of(row);

            DataRecord currentObjectRecord = DataRecord.of(stagingRecord.get(ImportStagingMeta.DATA));
            Map<String, Object> existingDataFromDb = existingFromDbByIdentity.get(currentObjectRecord.get(idAttribute.getFieldName(), UUID.class));

            //update version from db
            currentObjectRecord.with(versionAttribute.getFieldName(), DataRecord.of(existingDataFromDb).getOrDefault(versionAttribute.getFieldName(), Long.class, 0L));

            //set new object data
            stagingRecord.with(ImportStagingMeta.DATA, currentObjectRecord.getValues());

            updateData.add(stagingRecord);
        });
    }


//    public Condition buildIdentityCondition(
//            ObjectMetadata objectMetadata,
//            List<String> identityFields,
//            List<Map<String, Object>> rows) {
//
//        if (identityFields.isEmpty() || rows.isEmpty()) {
//            return DSL.falseCondition();
//        }
//
//        Field<?>[] tableFields = identityFields.stream()
//                .map(fieldName -> {
//                    Attribute attribute = objectMetadata.getAttributeByName(fieldName);
//                    return attribute.getField();
//                })
//                .toArray(Field[]::new);
//
//        RowN tableRow = DSL.row(tableFields);
//
//        List<RowN> values = new ArrayList<>();
//
//        for (Map<String, Object> row : rows) {
//            DataRecord dataRecord = DataRecord.of(row);
//            Map<String, Object> data = dataRecord.get(ImportStagingMeta.DATA);
//
//            Field<?>[] rowValues = IntStream.range(0, identityFields.size())
//                    .mapToObj(i -> {
//                        String fieldName = identityFields.get(i);
//                        Field<?> tableField = tableFields[i];
//
//                        Object value = data.get(fieldName);
//
//                        return DSL.val(value, tableField.getDataType());
//                    })
//                    .toArray(Field[]::new);
//
//            values.add(DSL.row(rowValues));
//        }
//
//        return tableRow.in(values);
//    }

}

package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.dataexchange.imports.keys.IdentityKeyGenerator;
import com.msm.core.objects.dataexchange.imports.model.IdentityKey;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportErrorData;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
import com.msm.core.objects.dataexchange.imports.model.ImportRow;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ImportValidationService {
    public static final String DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
    public static final String DUPLICATE_IN_PARENT = "DUPLICATE_IN_PARENT";
    private final com.msm.core.objects.dataexchange.imports.validation.ImportValidationService importValidationService;
    private final ActionExecutor actionExecutor;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ImportConfigService importConfigService;
    private final ImportJobService importJobService;


    public void processValidate(UUID importJobId, ObjectMetadata metadata, List<ImportRow> rows) {
        List<ImportErrorData> errors = new ArrayList<>();
        ImportContext context = importJobService.contextOf(importJobId);
        List<String> identityFields = importConfigService.getIdentityFields(metadata.getName(), context);

        for (ImportRow row : rows) {
            importValidationService.populate(metadata, row.rowData());
            ImportErrorData rowError = importValidationService.validateCreate(metadata, row);
            if(rowError != null) {
                errors.add(rowError);
            }
        }
        if (context != null) {
            errors.addAll(duplicateErrors(importJobId, metadata, context, identityFields, rows, errors));
        }

        if (!errors.isEmpty()) {
            List<Map<String, Object>> errorMap = errors.stream().map(importErrorData -> DataRecord
                    .of()
                    .with(ImportErrorMeta.IMPORT_ID, importJobId)
                    .with(ImportErrorMeta.ROW_NUMBER, importErrorData.rowNumber())
                    .with(ImportErrorMeta.ERROR_TYPE, importErrorData.errorType())
                    .with(ImportErrorMeta.FIELD_NAME, importErrorData.fieldName())
                    .with(ImportErrorMeta.ERROR_MESSAGE, importErrorData.message())
                    .with(ImportErrorMeta.ERROR_CODE, importErrorData.errorCode())
                    .with(ImportErrorMeta.DATA, importErrorData.data())
                    .getValues()
            ).collect(Collectors.toList());

            //insert batch error
            internalObjectQueryRepository.saveAll(
                    ImportErrorMeta.OBJECT_NAME,
                    errorMap
            );

            //incrementError
            internalObjectQueryRepository.updateWithExpressions(
                    ImportJobMeta.OBJECT_NAME,
                    ImportJobMeta.ID.getField().eq(importJobService.jobIdOf(importJobId)),
                    Map.of(
                            ImportJobMeta.ERROR_ROWS.getFieldName(),
                            ImportJobMeta.ERROR_ROWS.getField().add(errors.size())
                    )
            );
        }

        long validRows = rows.size() - errors.stream().map(ImportErrorData::rowNumber).distinct().count();

        //incrementValid
        internalObjectQueryRepository.updateWithExpressions(
                ImportJobMeta.OBJECT_NAME,
                ImportJobMeta.ID.getField().eq(importJobService.jobIdOf(importJobId)),
                Map.of(
                        ImportJobMeta.VALID_ROWS.getFieldName(),
                        ImportJobMeta.VALID_ROWS.getField().add(validRows)
                )
        );


        List<Map<String, Object>> importStateRows = rows
                .stream()
                .map(importRow -> {
                    IdentityKey identityKey = IdentityKeyGenerator.generate(importRow.rowData(), identityFields);
                    return DataRecord
                            .of()
                            .with(ImportStagingMeta.IMPORT_ID, importJobId)
                            .with(ImportStagingMeta.ROW_NUMBER, importRow.rowNumber())
                            .with(ImportStagingMeta.DATA, importRow.rowData())
                            .with(ImportStagingMeta.IDENTITY_KEY, identityKey.key())
                            .with(ImportStagingMeta.IDENTITY_LEVEL, identityKey.level())
                            .getValues();
                }).collect(Collectors.toList());

        //insert ImportStagingMeta
        internalObjectQueryRepository.insertBatch(
                ImportStagingMeta.OBJECT_NAME,
                importStateRows
        );

    }



    /**
     * Import into one header: a row whose identity (within the header) repeats a row of the file, earlier in this batch
     * or in an earlier batch (staging), or with APPEND matches an existing detail of the header. Rows already in error
     * are left out (one error row per row).
     */
    private List<ImportErrorData> duplicateErrors(UUID importId, ObjectMetadata metadata, ImportContext context,
                                                  List<String> identityFields, List<ImportRow> rows, List<ImportErrorData> rowErrors) {
        if (identityFields.size() < 2) {
            // no identity within the header (parent.identity empty): every row is a new detail
            return List.of();
        }
        Set<Long> rowsInError = rowErrors.stream().map(ImportErrorData::rowNumber).collect(Collectors.toSet());
        Map<Long, String> keys = new LinkedHashMap<>();
        for (ImportRow row : rows) {
            if (!rowsInError.contains(row.rowNumber())) {
                keys.put(row.rowNumber(), IdentityKeyGenerator.generate(row.rowData(), identityFields).key());
            }
        }
        if (keys.isEmpty()) {
            return List.of();
        }
        Set<String> staged = internalObjectQueryRepository.findByCondition(
                        ImportStagingMeta.OBJECT_NAME,
                        ImportStagingMeta.IMPORT_ID.getField().eq(importId)
                                .and(ImportStagingMeta.IDENTITY_KEY.getField().in(keys.values())),
                        List.of(ImportStagingMeta.IDENTITY_KEY.getFieldName()))
                .stream()
                .map(row -> (String) row.get(ImportStagingMeta.IDENTITY_KEY.getFieldName()))
                .collect(Collectors.toSet());
        Set<String> existing = context.mode() == ImportMode.APPEND
                ? existingKeys(metadata, context, identityFields)
                : Set.of();

        List<ImportErrorData> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<Long, Map<String, Object>> dataByRow = rows.stream()
                .collect(Collectors.toMap(ImportRow::rowNumber, ImportRow::rowData, (first, second) -> first));
        keys.forEach((rowNumber, key) -> {
            String fields = String.join(", ", identityFields.subList(1, identityFields.size()));
            if (staged.contains(key) || !seen.add(key)) {
                errors.add(new ImportErrorData(rowNumber, ErrorType.VALIDATION_ERROR, fields, DUPLICATE_IN_FILE,
                        "The file has this " + fields + " more than once", dataByRow.get(rowNumber)));
            } else if (existing.contains(key)) {
                errors.add(new ImportErrorData(rowNumber, ErrorType.VALIDATION_ERROR, fields, DUPLICATE_IN_PARENT,
                        "The " + context.parentObject() + " already has this " + fields, dataByRow.get(rowNumber)));
            }
        });
        return errors;
    }

    /** Identity keys of the header's existing details. */
    @SuppressWarnings("unchecked")
    private Set<String> existingKeys(ObjectMetadata metadata, ImportContext context, List<String> identityFields) {
        Field<Object> parentField = (Field<Object>) metadata.getAttributeByName(identityFields.getFirst()).getField();
        return internalObjectQueryRepository.findByCondition(
                        metadata.getName(),
                        parentField.eq(context.parentId()),
                        identityFields)
                .stream()
                .map(row -> IdentityKeyGenerator.generate(row, identityFields).key())
                .collect(Collectors.toSet());
    }

//    public void fillReferenceData(UUID importId, String objectName, List<Map<String, Object>> items) {
//
//        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
//        objectMetadata.getAttributes().forEach(attr -> {
//            if (AttributeRefHelper.hasRef(attr)) {
//                Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, objectName, attr, items);
//                items.forEach(itemMap -> {
//                    String attrName = attr.getFieldName();
//                    Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
//                    if(Utils.CL.isNotEmpty(objectCodeMap)) {
//                        String codeRef = String.valueOf(itemMap.get(attrName));
//                        Map<String, Object> objectRef = objectCodeMap.get(codeRef);
//                        if(objectRef != null) {
//                            Object idObj = objectRef.get(Constants.OBJECT_PK);
//                            itemMap.put(attrName, idObj);
//                            itemMap.put(Utils.STR.format(Constants.ATTRIBUTE_REF_TEMPLATE, attrName), objectRef);
//                        }
//                    }
//                });
//            }
//        });
//    }
//
//    private Map<String, Map<String, Map<String, Object>>> resolveAttributeRef(
//            UUID importId,
//            String importObjectName,
//            Attribute attribute,
//            List<Map<String, Object>> items
//    ) {
//        String objectCellResource = Utils.STR.format("{0}.{1}",  importObjectName, attribute.getFieldName());
//        AttributeReferenceContext attributeReferenceContext = AttributeReferenceContext.of(importId,  importObjectName, attribute, items);
//
//        ActionContext<AttributeReferenceContext> actionContext = ActionContext
//                .<AttributeReferenceContext>builder()
//                .resource(objectCellResource)
//                .action(ObjectActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
//                .payload(attributeReferenceContext)
//                .build();
//
//        return actionExecutor.execute(actionContext);
//    }

}

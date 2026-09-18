package com.msm.core.objects.imports;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import com.msm.core.objects.imports.model.ImportErrorData;
import com.msm.core.objects.imports.model.ImportRow;
import com.msm.core.objects.imports.reference.AttributeRefHelper;
import com.msm.core.objects.imports.validation.ImportValidationService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BatchValidationService {
    private final ImportValidationService importValidationService;
    private final ActionExecutor actionExecutor;
    private final ObjectQueryRepository internalObjectQueryRepository;



    public void processBatch(UUID importJobId, ObjectMetadata metadata, List<ImportRow> rows) {
        List<ImportErrorData> errors = new ArrayList<>();

        for (ImportRow row : rows) {
            importValidationService.populate(metadata, row.rowData());
            ImportErrorData rowError = importValidationService.validateCreate(metadata, row);
            if(rowError != null) {
                errors.add(rowError);
            }
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
            internalObjectQueryRepository.save(
                    ImportErrorMeta.OBJECT_NAME,
                    errorMap
            );

            //incrementError
            internalObjectQueryRepository.updateWithExpressions(
                    ImportJobMeta.OBJECT_NAME,
                    ImportJobMeta.ID.getField().eq(importJobId),
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
                ImportJobMeta.ID.getField().eq(importJobId),
                Map.of(
                        ImportJobMeta.VALID_ROWS.getFieldName(),
                        ImportJobMeta.VALID_ROWS.getField().add(validRows)
                )
        );

        List<Map<String, Object>> importStateRows = rows
                .stream()
                .map(importRow -> DataRecord
                        .of()
                        .with(ImportStagingMeta.IMPORT_ID, importJobId)
                        .with(ImportStagingMeta.ROW_NUMBER, importRow.rowNumber())
                        .with(ImportStagingMeta.DATA, importRow.rowData())
                        .getValues()
                ).collect(Collectors.toList());

        //insert ImportStagingMeta
        internalObjectQueryRepository.insertBatch(
                ImportStagingMeta.OBJECT_NAME,
                importStateRows
        );

    }



    public void fillReferenceData(UUID importId, String objectName, List<Map<String, Object>> items) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attr -> {
            if (AttributeRefHelper.hasRef(attr)) {
                Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, objectName, attr, items);
                items.forEach(itemMap -> {
                    String attrName = attr.getFieldName();
                    Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
                    if(Utils.CL.isNotEmpty(objectCodeMap)) {
                        String codeRef = String.valueOf(itemMap.get(attrName));
                        Map<String, Object> objectRef = objectCodeMap.get(codeRef);
                        if(objectRef != null) {
                            Object idObj = objectRef.get(Constants.OBJECT_PK);
                            itemMap.put(attrName, idObj);
                            itemMap.put(Utils.STR.format(Constants.ATTRIBUTE_REF_TEMPLATE, attrName), objectRef);
                        }
                    }
                });
            }
        });
    }

    private Map<String, Map<String, Map<String, Object>>> resolveAttributeRef(
            UUID importId,
            String importObjectName,
            Attribute attribute,
            List<Map<String, Object>> items
    ) {
        String objectCellResource = Utils.STR.format("{0}.{1}",  importObjectName, attribute.getFieldName());
        AttributeReferenceContext attributeReferenceContext = AttributeReferenceContext.of(importId,  importObjectName, attribute, items);

        ActionContext<AttributeReferenceContext> actionContext = ActionContext
                .<AttributeReferenceContext>builder()
                .resource(objectCellResource)
                .action(ObjectActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
                .payload(attributeReferenceContext)
                .build();

        return actionExecutor.execute(actionContext);
    }

}

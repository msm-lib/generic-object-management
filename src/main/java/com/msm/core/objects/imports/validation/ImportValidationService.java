package com.msm.core.objects.imports.validation;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.imports.model.ImportErrorData;
import com.msm.core.objects.imports.model.ImportRow;
import com.msm.core.validate.domain.MessageError;
import com.msm.core.validate.validation.AttributeValidator;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class ImportValidationService {
    private final AttributeValidator createAttributeValidator;
    private final AttributeValidator updateAttributeValidator;

    public void populate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        //fill free text and default value
        objectMetadata.getAttributes().forEach(attr -> {
            if(Boolean.TRUE.equals(attr.getIsFreeText())) {
                Object value = payload.get(attr.getFieldName());
                if(Objects.nonNull(value) && value instanceof String) {
                    payload.put(attr.getFieldName(), Utils.STR.normalizeText(String.valueOf(value)));
                }
            }
            if(Objects.nonNull(attr.getDefaultValue())) {
                Object currentValue = payload.get(attr.getFieldName());
                if(Objects.isNull(currentValue)) {
                    payload.put(attr.getFieldName(), attr.getDefaultValue());
                }
            }
        });
    }

    public ImportErrorData validateCreate(ObjectMetadata metadata, ImportRow row) {
        List<MessageError> errors = createAttributeValidator.validate(metadata, row.rowData());
        return toImportError(errors, row);
    }

    public ImportErrorData toImportError(List<MessageError> errors, ImportRow row) {

        if (errors == null || errors.isEmpty()) {
            return null;
        }
        java.util.StringJoiner codeJoiner = new java.util.StringJoiner(", ");
        java.util.StringJoiner labelJoiner = new java.util.StringJoiner(", ");
        java.util.StringJoiner attributeJoiner = new java.util.StringJoiner(", ");
        java.util.StringJoiner messageJoiner = new java.util.StringJoiner(", ");

        for (MessageError err : errors) {
            if (err.getCode() != null) {
                codeJoiner.add(err.getCode().getCode());
            }

            if (err.getLabel() != null && !err.getLabel().isEmpty()) {
                labelJoiner.add(err.getLabel());
            }

            if (err.getAttribute() != null && !err.getAttribute().isEmpty()) {
                attributeJoiner.add(err.getAttribute());
            }

            if (err.getMessage() != null && !err.getMessage().isEmpty()) {
                messageJoiner.add(err.getMessage());
            }
        }

        return new ImportErrorData(
                row.rowNumber(),
                "VALIDATION",
                attributeJoiner.toString(),
                codeJoiner.toString(),
                messageJoiner.toString(),
                row.rowData()
        );
    }
}

package com.msm.core.objects.service;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ObjectErrorDetail;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.validate.domain.MessageError;
import com.msm.core.validate.validation.AttributeValidator;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class ValidateAndPopulateDataService {

    private final AttributeValidator defaultAttributeValidator;

    public void validate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        populate(objectMetadata, payload);
        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectMetadata, payload);
        if(!messageErrors.isEmpty()) {
            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
            throw ObjectErrors.validateException(objectErrorDetails);
        }
    }

    public void validate(ObjectMetadata objectMetadata, List<Map<String, Object>> payload) {
        payload.forEach(objectPayload -> validate(objectMetadata, objectPayload));
    }

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
}

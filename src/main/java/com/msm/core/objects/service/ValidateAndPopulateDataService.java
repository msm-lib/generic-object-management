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

    private final AttributeValidator createAttributeValidator;
    private final AttributeValidator updateAttributeValidator;

//    public void populateAndValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
//        populate(objectMetadata, payload);
//        validate(objectMetadata, payload);
//    }
//
//    public void validate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
//        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectMetadata, payload);
//        if(!messageErrors.isEmpty()) {
//            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
//            throw ObjectErrors.validateException(objectErrorDetails);
//        }
//    }
//
//    public void populateAndValidate(ObjectMetadata objectMetadata, List<Map<String, Object>> payload) {
//        payload.forEach(objectPayload -> populateAndValidate(objectMetadata, objectPayload));
//    }

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




    public void createPopulateAndValidate(ObjectMetadata objectMetadata, List<Map<String, Object>> payload) {
        payload.forEach(objectPayload -> createPopulateAndValidate(objectMetadata, objectPayload));
    }

    public void updatePopulateAndValidate(ObjectMetadata objectMetadata, List<Map<String, Object>> payload) {
        payload.forEach(objectPayload -> updatePopulateAndValidate(objectMetadata, objectPayload));
    }

    public void createPopulateAndValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        populate(objectMetadata, payload);
        createValidate(objectMetadata, payload);
    }

    public void updatePopulateAndValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        populate(objectMetadata, payload);
        updateValidate(objectMetadata, payload);
    }

    public void createValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        List<MessageError> messageErrors =  createAttributeValidator.validate(objectMetadata, payload);
        if(!messageErrors.isEmpty()) {
            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
            throw ObjectErrors.validateException(objectErrorDetails);
        }
    }

    public void createValidate(ObjectMetadata objectMetadata, List<Map<String, Object>> payloads) {
        payloads.forEach(objectPayload -> createValidate(objectMetadata, objectPayload));
    }

    public void updateValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        List<MessageError> messageErrors =  updateAttributeValidator.validate(objectMetadata, payload);
        if(!messageErrors.isEmpty()) {
            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
            throw ObjectErrors.validateException(objectErrorDetails);
        }
    }

    public void updateValidate(ObjectMetadata objectMetadata, List<Map<String, Object>> payloads) {
        payloads.forEach(objectPayload -> updateValidate(objectMetadata, objectPayload));
    }

}

package com.msm.core.objects.hook;

import com.msm.core.action.annotations.hook.crud.HookBeforeCreate;
import com.msm.core.action.annotations.hook.crud.HookBeforeUpdate;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ErrorDetail;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.validate.domain.MessageError;
import com.msm.core.validate.validation.AttributeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GenericHookEvent {
    private final AttributeValidator defaultAttributeValidator;
    private final GenericObjectMetadataService genericObjectMetadataService;

    private void simpleFormula(ActionContext<Map<String, Object>> ctx, ObjectMetadata objectMetadata) {
        //fill free text and default value
        Map<String, Object> payloadMap = ctx.getPayload();
        objectMetadata.getAttributes().forEach(attr -> {
            if(Boolean.TRUE.equals(attr.getIsFreeText())) {
                Object value = payloadMap.get(attr.getFieldName());
                if(Objects.nonNull(value) && value instanceof String) {
                    payloadMap.put(attr.getFieldName(), Utils.STR.normalizeText(String.valueOf(value)));
                }
            }
            if(Objects.nonNull(attr.getDefaultValue())) {
                Object currentValue = payloadMap.get(attr.getFieldName());
                if(Objects.isNull(currentValue)) {
                    payloadMap.put(attr.getFieldName(), attr.getDefaultValue());
                }
            }
        });
    }

    private void validate(ActionContext<Map<String, Object>> ctx) {
        Optional<ObjectMetadata> objectAttribute = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectAttribute.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }
        simpleFormula(ctx, objectAttribute.get());
        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectAttribute.get(), ctx.getPayload());
        log.info(messageErrors.toString());
        if(!messageErrors.isEmpty()) {
            List<ErrorDetail> errorDetails = messageErrors.stream().map(msg -> ErrorDetail.create(msg.getCode(),msg.getMessage())).toList();
            throw Errors.throwException(errorDetails);
        }
    }

    @HookBeforeCreate
    public void beforeEvent(ActionContext<Map<String, Object>> ctx) {
        validate(ctx);
        log.info("[BEFORE_CREATE] Generic before create object: {}", ctx.getObjectId());
    }

    @HookBeforeUpdate
    public void hookBeforeUpdate(ActionContext<Map<String, Object>> ctx) {
        validate(ctx);
        log.info("[BEFORE_UPDATE] Generic before update object: {}", ctx);
    }
}

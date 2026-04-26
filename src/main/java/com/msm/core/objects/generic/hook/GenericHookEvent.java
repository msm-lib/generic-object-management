package com.msm.core.objects.generic.hook;

import com.msm.core.commons.Utils;
import com.msm.core.hook.anontation.crud.HookAfterCommitCreate;
import com.msm.core.hook.anontation.crud.HookAfterCreate;
import com.msm.core.hook.anontation.crud.HookBeforeCreate;
import com.msm.core.hook.context.ActionRequest;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ErrorDetail;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.generic.service.GenericAttributeService;
import com.msm.core.validate.domain.MessageError;
import com.msm.core.validate.validation.AttributeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericHookEvent {
    private final AttributeValidator defaultAttributeValidator;
    private final GenericAttributeService genericAttributeService;

    private void simpleFormula(ActionRequest<Map<String, Object>> ctx) {
        //fill free text and default value
        Optional<ObjectMetadata> objectAttribute = genericAttributeService.getObjectAttribute(ctx.getObjectName());
        if(objectAttribute.isEmpty()) {
            return;
        }
        Map<String, Object> payloadMap = ctx.getPayload();
        objectAttribute.get().getAttributes().forEach(attr -> {
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

    @HookBeforeCreate
    public void beforeEvent(ActionRequest<Map<String, Object>> ctx) {
        Optional<ObjectMetadata> objectAttribute = genericAttributeService.getObjectAttribute(ctx.getObjectName());
        if(objectAttribute.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getObjectName());
            return;
        }

        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectAttribute.get(), ctx.getPayload());
        log.info(messageErrors.toString());
        if(!messageErrors.isEmpty()) {
            List<ErrorDetail> errorDetails = messageErrors.stream().map(msg -> ErrorDetail.create(msg.getCode(),msg.getMessage())).toList();
            throw Errors.throwException(errorDetails);
        }
        simpleFormula(ctx);
        log.info("[BEFORE_EVENT] Generic before create object: {}", ctx.getObjectId());
    }

    @HookAfterCreate
    public void afterEvent(ActionRequest<Map<String, Object>> ctx) {
        log.info("[AFTER_EVENT] Object created: {}", ctx);
    }

    @HookAfterCommitCreate
    public void afterCommitEvent(ActionRequest<Map<String, Object>> ctx) {
        log.info("[AFTER_COMMIT_EVENT] Object created: {}", ctx);
    }
}

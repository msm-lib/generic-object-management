package com.msm.core.objects.hook.system;

import com.msm.core.action.annotations.hook.system.HookSystemBefore;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ErrorDetail;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.ObjectUsageConfig;
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
public class SystemHookEvent {
    private final ObjectUsageConfig objectUsageConfig;
    private final AttributeValidator defaultAttributeValidator;
    private final GenericObjectMetadataService genericObjectMetadataService;

    @HookSystemBefore(action = Constants.Action.CREATE, order = Integer.MIN_VALUE)
    public void HookSystemBeforeCreate(ActionContext<Map<String, Object>> ctx) {
        validate(ctx);
    }
    @HookSystemBefore(action = Constants.Action.UPDATE, order = Integer.MIN_VALUE)
    public void HookSystemBeforeUpdate(ActionContext<Map<String, Object>> ctx) {
        validate(ctx);
    }

//    @HookAfterCreate(resource = Constants.GENERIC_SYSTEM_OBJECT)
//    public void HookAfterCreate(ActionContext<Map<String, Object>> ctx) {
////        objectUsageService.saveObjectUsage(ctx);
////        log.info("[BEFORE_EVENT] Generic before create object: {}", ctx.getObjectId());
//    }
//
//    @HookAfterCommitCreate(resource = Constants.GENERIC_SYSTEM_OBJECT)
//    public void HookAfterCommitCreate(ActionContext<Map<String, Object>> ctx) {
////        objectUsageService.sendEvent(ctx);
////        log.info("[BEFORE_EVENT] Generic before create object: {}", ctx.getObjectId());
//    }


    private void populate(ActionContext<Map<String, Object>> ctx, ObjectMetadata objectMetadata) {
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
        populate(ctx, objectAttribute.get());
        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectAttribute.get(), ctx.getPayload());
        log.info(messageErrors.toString());
        if(!messageErrors.isEmpty()) {
            List<ErrorDetail> errorDetails = messageErrors.stream().map(msg -> ErrorDetail.create(msg.getCode(),msg.getMessage())).toList();
            throw Errors.throwException(errorDetails);
        }
    }
}

package com.msm.core.objects.hook.system;

import com.msm.core.action.annotations.hook.system.HookSystemBefore;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ObjectErrorDetail;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.ObjectDependencyService;
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
    private final ObjectDependencyService objectDependencyService;
    private final AttributeValidator defaultAttributeValidator;
    private final GenericObjectMetadataService genericObjectMetadataService;

//    @HookSystemAfter(action = Constants.Action.CREATE, order = Integer.MIN_VALUE)
//    public void hookSystemAfterCreate(ActionContext<Map<String, Object>> ctx) {
//        objectDependencyService.saveObjectDependency(ctx);
//    }
//
//    @HookSystemAfterCommit(action = Constants.Action.CREATE, order = Integer.MIN_VALUE)
//    public void hookSystemAfterCommitCreate(ActionContext<Map<String, Object>> ctx) {
//        objectDependencyService.sendEvent(ctx);
//    }


    @HookSystemBefore(action = Constants.Action.CREATE, order = Integer.MIN_VALUE)
    public void hookSystemBeforeCreate(ActionContext<Map<String, Object>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }
        validate(objectMetadataOptional.get(), ctx.getPayload());
    }

    @HookSystemBefore(action = Constants.Action.BULK_CREATE, order = Integer.MIN_VALUE)
    public void hookSystemBeforeBulkCreate(ActionContext<List<Map<String, Object>>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }
        ctx.getPayload().forEach(payload -> validate(objectMetadataOptional.get(), payload));

    }

    @HookSystemBefore(action = Constants.Action.UPDATE, order = Integer.MIN_VALUE)
    public void HookSystemBeforeUpdate(ActionContext<Map<String, Object>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }
        validate(objectMetadataOptional.get(), ctx.getPayload());
    }

    private void validate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        populate(objectMetadata, payload);
        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectMetadata, payload);
        if(!messageErrors.isEmpty()) {
            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
            throw ObjectErrors.validateException(objectErrorDetails);
        }
    }

    private void populate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
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

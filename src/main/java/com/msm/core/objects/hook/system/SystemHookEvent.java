package com.msm.core.objects.hook.system;

import com.msm.core.action.annotations.hook.system.HookSystemBefore;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.ValidateAndPopulateDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SystemHookEvent {

    private final GenericObjectMetadataService genericObjectMetadataService;
    private final ValidateAndPopulateDataService validateAndPopulateDataService;


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

        validateAndPopulateDataService.createPopulateAndValidate(objectMetadataOptional.get(), ctx.getPayload());
    }

    @HookSystemBefore(action = Constants.Action.BULK_CREATE, order = Integer.MIN_VALUE)
    public void hookSystemBeforeBulkCreate(ActionContext<List<Map<String, Object>>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }

        validateAndPopulateDataService.createPopulateAndValidate(objectMetadataOptional.get(), ctx.getPayload());
    }

    @HookSystemBefore(action = Constants.Action.UPDATE, order = Integer.MIN_VALUE)
    public void hookSystemBeforeUpdate(ActionContext<Map<String, Object>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }

        validateAndPopulateDataService.updateValidate(objectMetadataOptional.get(), ctx.getPayload());
    }

    @HookSystemBefore(action = Constants.Action.BULK_UPDATE, order = Integer.MIN_VALUE)
    public void hookSystemBeforeBulkUpdate(ActionContext<List<Map<String, Object>>> ctx) {
        Optional<ObjectMetadata> objectMetadataOptional = genericObjectMetadataService.getObjectMetadata(ctx.getResource());
        if(objectMetadataOptional.isEmpty()) {
            log.warn("No object attribute found with name {}", ctx.getResource());
            return;
        }

        validateAndPopulateDataService.updateValidate(objectMetadataOptional.get(), ctx.getPayload());
    }

//    private void populateAndValidate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
//        populate(objectMetadata, payload);
//        validate(objectMetadata, payload);
//    }
//
//    private void validate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
//        List<MessageError> messageErrors =  defaultAttributeValidator.validate(objectMetadata, payload);
//        if(!messageErrors.isEmpty()) {
//            List<ObjectErrorDetail> objectErrorDetails = messageErrors.stream().map(msg -> ObjectErrorDetail.create(msg.getCode(), Map.of("attribute", msg.getAttribute()), msg.getMessage())).toList();
//            throw ObjectErrors.validateException(objectErrorDetails);
//        }
//    }
//
//    private void populate(ObjectMetadata objectMetadata, Map<String, Object> payload) {
//        //fill free text and default value
//        objectMetadata.getAttributes().forEach(attr -> {
//            if(Boolean.TRUE.equals(attr.getIsFreeText())) {
//                Object value = payload.get(attr.getFieldName());
//                if(Objects.nonNull(value) && value instanceof String) {
//                    payload.put(attr.getFieldName(), Utils.STR.normalizeText(String.valueOf(value)));
//                }
//            }
//            if(Objects.nonNull(attr.getDefaultValue())) {
//                Object currentValue = payload.get(attr.getFieldName());
//                if(Objects.isNull(currentValue)) {
//                    payload.put(attr.getFieldName(), attr.getDefaultValue());
//                }
//            }
//        });
//    }
}

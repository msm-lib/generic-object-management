package com.msm.core.objects.generic.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.hook.anontation.Handler;
import com.msm.core.hook.context.ActionRequest;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.exception.ServiceErrorEnum;
import com.msm.core.objects.generic.audit.AuditAction;
import com.msm.core.objects.generic.audit.AuditStrategy;
import com.msm.core.objects.generic.converter.ObjectMappingStrategy;
import com.msm.core.objects.generic.service.GenericObjectMetadataService;
import com.msm.core.strategy.StrategyResolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked"})
@RequiredArgsConstructor
public class GenericObjectHandler {
    private final DynamicQueryService dynamicQueryService;
    private final GenericObjectMetadataService genericObjectMetadataService;
    private final StrategyResolver<AuditStrategy> auditStrategyFactory;
    private final StrategyResolver<ObjectMappingStrategy> objectMappingStrategyFactory;

    @Handler(action = Constants.GENERIC_FILTER_ACTION)
    public <X> X filter(ActionRequest<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        PageResponse<Object> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        mapFrom(objectMetadata, pageResponse.getContents());
        return (X) pageResponse;
    }

    @Handler(action = Constants.GENERIC_ALL_OBJECT_ACTION)
    public <X> X findAllObject(ActionRequest<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        PageResponse<Object> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        mapFrom(objectMetadata, pageResponse.getContents());
        return (X) pageResponse.getContents();
    }

    @Handler(action = Constants.GENERIC_FILTER_BY_ID_ACTION)
    public <X> X findObjectById(ActionRequest<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        PageResponse<Object> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        List<Object> objects = pageResponse.getContents();
        mapFrom(objectMetadata, objects);
        return (X) Utils.CL.getFirst(objects);
    }

    @Handler(action = Constants.Action.CREATE)
    public <X> X create(ActionRequest<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        mapTo(objectMetadata, request.getPayload());
        applyAudit(objectMetadata, AuditAction.CREATE, request.getPayload());
        Map<String, Object> returnObject = dynamicQueryService.insertReturning(objectMetadata, request.getPayload());
        mapFrom(objectMetadata, returnObject);
        return (X) returnObject;
    }

    @Handler(action = Constants.Action.UPDATE)
    public <X> X update(ActionRequest<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        Map<String, Object> newData = request.getPayload();
        Map<String, Object> oldData = dynamicQueryService.findById(objectMetadata, request.getObjectId(), null);
        if (oldData == null) {
            throw Errors.throwException(ServiceErrorEnum.OBJECT_NOT_FOUND, request.getObjectId());
        }
        mapFrom(objectMetadata, oldData);
        try {
            Utils.O.updateValues(oldData, newData);
        } catch (JsonMappingException e) {
            throw Errors.throwException(ServiceErrorEnum.INVALID_UPDATE_PAYLOAD, e.getOriginalMessage());
        }
        mapTo(objectMetadata, oldData);
        applyAudit(objectMetadata, AuditAction.UPDATE, oldData);
        dynamicQueryService.updateById(objectMetadata, request.getObjectId(), oldData);
        mapFrom(objectMetadata, oldData);
        return (X) oldData;
    }

    @Handler(action = Constants.Action.DELETE)
    public <X> X delete(ActionRequest<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getObjectName());
        applyAudit(objectMetadata, AuditAction.DELETE, request.getPayload());
        Object rowEffected = dynamicQueryService.deleteById(objectMetadata, request.getObjectId());
        return (X) rowEffected;
    }

    private ObjectMetadata getObjectMetadata(String objectName) {
        return genericObjectMetadataService
                .getObjectMetadata(objectName)
                .orElseThrow(() -> Errors.throwException(ServiceErrorEnum.OBJECT_META_DATA_NOT_FOUND));
    }

    private void mapFrom(ObjectMetadata objectMetadata, List<Object> objects) {
        Utils.CL.emptyIfNull(objects).forEach(o -> {
            mapFrom(objectMetadata, (Map<String, Object>) o);
        });
    }

    private void mapFrom(ObjectMetadata objectMetadata, Map<String, Object> object) {
        objectMappingStrategyFactory
                .resolve(objectMetadata.getName())
                .from(objectMetadata, object);
    }

    private void mapTo(ObjectMetadata objectMetadata, Map<String, Object> payload) {
        objectMappingStrategyFactory
                .resolve(objectMetadata.getName())
                .to(objectMetadata, payload);
    }

    private void applyAudit(ObjectMetadata objectMetadata, AuditAction action, Map<String, Object> data) {
        auditStrategyFactory
                .resolve(objectMetadata.getName())
                .apply(action, objectMetadata, data);
    }
}

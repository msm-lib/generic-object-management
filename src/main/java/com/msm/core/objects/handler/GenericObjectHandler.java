package com.msm.core.objects.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.audit.AuditAction;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.strategy.StrategyResolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class GenericObjectHandler {
    private final DynamicQueryService dynamicQueryService;
    private final GenericObjectMetadataService genericObjectMetadataService;
    private final StrategyResolver<String, AuditStrategy> auditStrategyFactory;
    private final StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory;

    @Handler(action = Constants.FilterAction.FILTER_OBJECT)
    public PageResponse<Map<String, Object>> filter(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    @Handler(action = Constants.FilterAction.FILTER_ALL_OBJECT)
    public List<Map<String, Object>> findAllObject(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse.getContents();
    }

    @Handler(action = Constants.FilterAction.FILTER_OBJECT_BY_ID)
    public Map<String, Object> findObjectById(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return Utils.CL.getFirst(pageResponse.getContents());
    }

    @Handler(action = Constants.FilterAction.FILTER_ALL_OBJECT_BY_IDS)
    public List<Map<String, Object>> findAllObjectByIds(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = dynamicQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse.getContents();
    }

    @Handler(action = Constants.Action.CREATE)
    public Map<String, Object> create(ActionContext<Map<String, Object>> request) {
        Object code = request.getPayload().get("code");
        if(Objects.isNull(code)) {
            String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(request.getResource())), () -> "");
            int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
            request.getPayload().put("code", Utils.toCodeGenerator(prefix, len));
        }
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        applyAudit(objectMetadata, AuditAction.CREATE, request.getPayload());
        mapTo(objectMetadata, request.getPayload());
        Map<String, Object> returnObject = dynamicQueryService.insertReturning(objectMetadata, request.getPayload());
        mapFrom(objectMetadata, returnObject);
        return returnObject;
    }

    @Handler(action = Constants.Action.BULK_CREATE)
    public List<Map<String, Object>> bulkCreate(ActionContext<List<Map<String, Object>>> request) {
        request.getPayload().forEach(objectMap -> {
            Object code = objectMap.get("code");
            if(Objects.isNull(code)) {
                String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(request.getResource())), () -> "");
                int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
                objectMap.put("code", Utils.toCodeGenerator(prefix, len));
            }
        });

        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        request.getPayload().forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = dynamicQueryService.insertReturning(objectMetadata, request.getPayload());
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
    }

    @Handler(action = Constants.Action.UPDATE)
    public Map<String, Object> update(ActionContext<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        Map<String, Object> newData = request.getPayload();
        Map<String, Object> oldData = dynamicQueryService.findById(objectMetadata, request.getObjectId(), null);
        if (oldData == null) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        mapFrom(objectMetadata, oldData);
        try {
            Utils.O.updateValues(oldData, newData);
        } catch (JsonMappingException e) {
            throw ObjectErrors.payloadInvalidException(objectMetadata.getName(), e.getMessage(), e);
        }
        applyAudit(objectMetadata, AuditAction.UPDATE, oldData);
        mapTo(objectMetadata, oldData);
        Map<String, Object> returnValueUpdated = dynamicQueryService.updateById(objectMetadata, request.getObjectId(), oldData);
        mapFrom(objectMetadata, returnValueUpdated);
        return returnValueUpdated;
    }

    @Handler(action = Constants.Action.DELETE)
    public int delete(ActionContext<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        if(!isSoftDeleted(objectMetadata)) {
            return dynamicQueryService.forceDeleteById(objectMetadata, request.getObjectId());
        }
        applyAudit(objectMetadata, AuditAction.DELETE, request.getPayload());
        return dynamicQueryService.deleteById(objectMetadata, request.getObjectId(), request.getPayload());
    }

    public List<Map<String, Object>> bulkCreateIgnoreConflictOnConstraintName(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName) {
        request.forEach(objectMap -> {
            Object code = objectMap.get("code");
            if(Objects.isNull(code)) {
                String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(objectName)), () -> "");
                int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
                objectMap.put("code", Utils.toCodeGenerator(prefix, len));
            }
        });

        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = dynamicQueryService.insertReturningInsertedRows(objectMetadata, request, conflictOnConstraintName);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
    }

    public Map<String, Object> createIgnoreConflictOnConstraintName(String objectName, Map<String, Object> request, String conflictOnConstraintName) {
        Object code = request.get("code");
        if(Objects.isNull(code)) {
            String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(objectName)), () -> "");
            int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
            request.put("code", Utils.toCodeGenerator(prefix, len));
        }
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        applyAudit(objectMetadata, AuditAction.CREATE, request);
        mapTo(objectMetadata, request);
        Map<String, Object> returnObject = dynamicQueryService.insertReturningInsertedRow(objectMetadata, request, conflictOnConstraintName);
        mapFrom(objectMetadata, returnObject);
        return returnObject;
    }

    private ObjectMetadata getObjectMetadata(String objectName) {
        return genericObjectMetadataService
                .getObjectMetadata(objectName)
                .orElseThrow(() -> ObjectErrors.notFound(objectName));
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

    private boolean isSoftDeleted(ObjectMetadata meta) {
        Attribute deletedAt = meta.getAttributeByName(Constants.DELETED_AT);
        Attribute deletedBy = meta.getAttributeByName(Constants.DELETED_BY);
        Attribute deletedById = meta.getAttributeByName(Constants.DELETED_BY_ID);
        Attribute isDeleted = meta.getAttributeByName(Constants.IS_DELETED);
        return isDeleted != null || deletedAt != null || deletedBy != null || deletedById != null;
    }
}

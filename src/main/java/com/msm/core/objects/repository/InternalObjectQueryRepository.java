package com.msm.core.objects.repository;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.audit.AuditAction;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.strategy.StrategyResolver;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class InternalObjectQueryRepository implements ObjectQueryRepository {
    private final ObjectQuery internalQueryService;
    private final GenericObjectMetadataService genericObjectMetadataService;
    private final StrategyResolver<String, AuditStrategy> auditStrategyFactory;
    private final StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory;

    public PageResponse<Map<String, Object>> lookup(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = internalQueryService.lookup(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    public PageResponse<Map<String, Object>> lookup(String objectName, ObjectFilterRequest request) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        PageResponse<Map<String, Object>> pageResponse = internalQueryService.lookup(objectMetadata, request);
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    public PageResponse<Map<String, Object>> filter(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = internalQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    public List<Map<String, Object>> findObject(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = internalQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse.getContents();
    }

    public Map<String, Object> findById(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = internalQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return Utils.CL.getFirst(pageResponse.getContents());
    }

    public Map<String, Object> save(String objectName, Map<String, Object> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        applyAudit(objectMetadata, AuditAction.CREATE, payload);
        mapTo(objectMetadata, payload);
        Map<String, Object> returnObject = internalQueryService.insertReturning(objectMetadata, payload);
        mapFrom(objectMetadata, returnObject);
        return returnObject;
    }

    public List<Map<String, Object>> save(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        payload.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = internalQueryService.insertReturning(objectMetadata, payload);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
    }

    public int update(String objectName, Object id, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> oldData = internalQueryService.findById(objectMetadata, id, null);
        if (oldData == null) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        updateData(objectMetadata, oldData, newData);
        return internalQueryService.updateById(objectMetadata, id, oldData);
    }

    public int[] update(String objectName, List<Map<String, Object>> newDataList) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Attribute idAttribute = objectMetadata.getIdAttribute();
        String idFieldName = idAttribute.getFieldName();

        List<Object> ids = newDataList
                .stream()
                .map(oldData -> oldData.get(idFieldName))
                .collect(Collectors.toList());
        List<Map<String, Object>> oldDataList = internalQueryService.findByIds(objectMetadata, ids);
        if (Utils.CL.isEmpty(oldDataList)) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        Map<Object, Map<String, Object>> oldDataListMapById = Utils.CL.toMap(oldDataList, dataKey -> dataKey.get(idFieldName), dataValue -> dataValue);

        newDataList.forEach(newObjectMap -> {
            Object id = newObjectMap.get(idFieldName);
            Map<String, Object> oldData = oldDataListMapById.get(idAttribute.cast(id));
            updateData(objectMetadata, oldData, newObjectMap);
        });
        return internalQueryService.batchUpdate(objectMetadata, oldDataList);
    }

    public Map<String, Object> updateReturning(String objectName, Object id, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> oldData = internalQueryService.findById(objectMetadata, id, null);
        if (oldData == null) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        updateData(objectMetadata, oldData, newData);
        Map<String, Object> returnValueUpdated = internalQueryService.updateByIdReturning(objectMetadata, id, oldData);
        mapFrom(objectMetadata, returnValueUpdated);
        return returnValueUpdated;
    }

    public List<Map<String, Object>> updateReturning(String objectName, List<Map<String, Object>> newDataList) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Attribute idAttribute = objectMetadata.getIdAttribute();
        String idFieldName = idAttribute.getFieldName();

        List<Object> ids = newDataList
                .stream()
                .map(oldData -> oldData.get(idFieldName))
                .collect(Collectors.toList());
        List<Map<String, Object>> oldDataList = internalQueryService.findByIds(objectMetadata, ids);
        if (Utils.CL.isEmpty(oldDataList)) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        Map<Object, Map<String, Object>> oldDataListMapById = Utils.CL.toMap(oldDataList, dataKey -> dataKey.get(idFieldName), dataValue -> dataValue);

        newDataList.forEach(newObjectMap -> {
            Object id = newObjectMap.get(idFieldName);
            Map<String, Object> oldData = oldDataListMapById.get(idAttribute.cast(id));
            updateData(objectMetadata, oldData, newObjectMap);
        });
        List<Map<String, Object>> dataUpdatedList =  internalQueryService.updateReturning(objectMetadata, oldDataList);

        Utils.CL.emptyIfNull(dataUpdatedList).forEach(dataUpdated -> mapFrom(objectMetadata, dataUpdated));

        return dataUpdatedList;
    }

    private void updateData(ObjectMetadata objectMetadata, Map<String, Object> oldData, Map<String, Object> newData) {
        mapFrom(objectMetadata, oldData);
        try {
            Utils.O.updateValues(oldData, newData);
        } catch (JsonMappingException e) {
            throw ObjectErrors.payloadInvalidException(objectMetadata.getName(), e.getMessage(), e);
        }
        applyAudit(objectMetadata, AuditAction.UPDATE, oldData);
        mapTo(objectMetadata, oldData);
    }

    public int delete(String objectName, Object id, Map<String, Object> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        if(!isSoftDeleted(objectMetadata)) {
            return internalQueryService.forceDeleteById(objectMetadata, id);
        }
        applyAudit(objectMetadata, AuditAction.DELETE, payload);
        return internalQueryService.deleteById(objectMetadata, id, payload);
    }

    public int delete(String objectName, Object id, Long version) {
        return delete(objectName, id, Utils.CL.newHashMap(Constants.VERSION, version));
    }

    public int delete(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        if(!isSoftDeleted(objectMetadata)) {
            return internalQueryService.forceDeleteByIds(objectMetadata, payload.stream().map(map -> map.get(Constants.OBJECT_PK)).collect(Collectors.toList()));
        }
        Utils.CL.emptyIfNull(payload).forEach(objectMap -> applyAudit(objectMetadata, AuditAction.DELETE, objectMap));
        return internalQueryService.delete(objectMetadata, payload);
    }

    public int[] batchDelete(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        if(!isSoftDeleted(objectMetadata)) {
            return internalQueryService.batchForceDeleteByIds(objectMetadata, payload.stream().map(map -> map.get(Constants.OBJECT_PK)).collect(Collectors.toList()));
        }
        Utils.CL.emptyIfNull(payload).forEach(objectMap -> applyAudit(objectMetadata, AuditAction.DELETE, objectMap));
        return internalQueryService.batchDelete(objectMetadata, payload);
    }

    public Map<String, Object> findById(String objectName, Object id, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = internalQueryService.findById(objectMetadata, id, returnFields);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public Map<String, Object> findById(String objectName, Object id) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = internalQueryService.findById(objectMetadata, id);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public List<Map<String, Object>> findByIds(String objectName, List<Object> ids) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        return findByIds(objectName, ids, objectMetadata.getFieldNames());
    }

    public List<Map<String, Object>> findByIds(String objectName, List<Object> ids, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> results = internalQueryService.findByIds(objectMetadata, ids, returnFields);
        Utils.CL.emptyIfNull(results).forEach(object -> mapFrom(objectMetadata, object));
        return results;
    }

    public Map<String, Object> findByCondition(String objectName, Condition condition) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = internalQueryService.findOneByCondition(objectMetadata, condition);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public Map<String, Object> findByCondition(String objectName, Condition condition, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = internalQueryService.findOneByCondition(objectMetadata, condition, returnFields);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public List<Map<String, Object>> findAllByCondition(String objectName, Condition condition) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> objectMap = internalQueryService.findByCondition(objectMetadata, condition);
        Utils.CL.emptyIfNull(objectMap).forEach(object -> mapFrom(objectMetadata, object));
        return objectMap;
    }

    public List<Map<String, Object>> findAllByCondition(String objectName, Condition condition, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> objectMap = internalQueryService.findByCondition(objectMetadata, condition, returnFields);
        Utils.CL.emptyIfNull(objectMap).forEach(object -> mapFrom(objectMetadata, object));
        return objectMap;
    }

    public List<Map<String, Object>> bulkCreateIgnoreConflictOnConstraintName(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName) {

        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = internalQueryService.insertReturningInsertedRows(objectMetadata, request, conflictOnConstraintName);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
    }

    public Map<String, Object> createIgnoreConflictOnConstraintName(String objectName, Map<String, Object> request, String conflictOnConstraintName) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        applyAudit(objectMetadata, AuditAction.CREATE, request);
        mapTo(objectMetadata, request);
        Map<String, Object> returnObject = internalQueryService.insertReturningInsertedRow(objectMetadata, request, conflictOnConstraintName);
        mapFrom(objectMetadata, returnObject);
        return returnObject;
    }

    public List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = internalQueryService.upsertReturning(objectMetadata, request, conflictOnConstraintName);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
    }

    public List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, List<String> conflictFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = internalQueryService.upsertReturning(objectMetadata, request, conflictFields);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));

        return returnObjects;
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

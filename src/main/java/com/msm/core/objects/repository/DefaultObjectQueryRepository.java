package com.msm.core.objects.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.filter.domain.pageable.Sort;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.audit.AuditAction;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.changelog.ChangeLogDiff;
import com.msm.core.objects.audit.changelog.ChangeLogRecorder;
import com.msm.core.objects.audit.changelog.UniqueConstraintColumns;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.strategy.StrategyResolver;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads and writes generic objects with an {@link ObjectQuery}: the secured one for {@code objectQueryRepository}, the
 * internal one for {@code internalObjectQueryRepository} (same class, two beans).
 *
 * <p>Every write of an object with a change log ({@link ChangeLogRecorder#isEnabled}) is recorded with the row before and
 * after it. The rows a write touches without reading them first (update / delete by condition, upsert, delete by ids)
 * are read before and after it with {@code changeLogQuery}, which has no data-scope filter, so a row outside the
 * caller's scope is logged too.
 */
public class DefaultObjectQueryRepository implements ObjectQueryRepository {
    private final ObjectQuery defaultQueryService;
    private final GenericObjectMetadataService genericObjectMetadataService;
    private final StrategyResolver<String, AuditStrategy> auditStrategyFactory;
    private final StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory;
    private final ChangeLogRecorder changeLogRecorder;
    private final ObjectQuery changeLogQuery;
    private final UniqueConstraintColumns uniqueConstraintColumns;
    private final ChangeLogDiff changeLogDiff = new ChangeLogDiff();

    public DefaultObjectQueryRepository(ObjectQuery defaultQueryService,
                                        GenericObjectMetadataService genericObjectMetadataService,
                                        StrategyResolver<String, AuditStrategy> auditStrategyFactory,
                                        StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory,
                                        ChangeLogRecorder changeLogRecorder,
                                        ObjectQuery changeLogQuery,
                                        UniqueConstraintColumns uniqueConstraintColumns) {
        this.defaultQueryService = defaultQueryService;
        this.genericObjectMetadataService = genericObjectMetadataService;
        this.auditStrategyFactory = auditStrategyFactory;
        this.objectMappingStrategyFactory = objectMappingStrategyFactory;
        this.changeLogRecorder = changeLogRecorder;
        this.changeLogQuery = changeLogQuery;
        this.uniqueConstraintColumns = uniqueConstraintColumns;
    }

    /** Without change log. */
    public DefaultObjectQueryRepository(ObjectQuery defaultQueryService,
                                        GenericObjectMetadataService genericObjectMetadataService,
                                        StrategyResolver<String, AuditStrategy> auditStrategyFactory,
                                        StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory) {
        this(defaultQueryService, genericObjectMetadataService, auditStrategyFactory, objectMappingStrategyFactory,
                ChangeLogRecorder.DISABLED, defaultQueryService, null);
    }

    public PageResponse<Map<String, Object>> lookup(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = defaultQueryService.lookup(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    public PageResponse<Map<String, Object>> lookup(String objectName, ObjectFilterRequest request) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        PageResponse<Map<String, Object>> pageResponse = defaultQueryService.lookup(objectMetadata, request);
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    public PageResponse<Map<String, Object>> filter(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = defaultQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse;
    }

    @Override
    public Stream<Map<String, Object>> filterStream(String objectName, ObjectFilterRequest request, int fetchSize) {
        return defaultQueryService.filterStream(
                getObjectMetadata(objectName),
                request,
                fetchSize
        );
    }

    @Override
    public <T> void filterStream(String objectName, ObjectFilterRequest request, int fetchSize, Class<T> targetClass, Consumer<T> consumer) {
        defaultQueryService.filterStream(getObjectMetadata(objectName), request, fetchSize, targetClass, consumer);
    }

    @Override
    public <T> void filterStream(String objectName, ObjectFilterRequest request, int fetchSize, TypeReference<T> targetType, Consumer<T> consumer) {
        defaultQueryService.filterStream(getObjectMetadata(objectName), request, fetchSize, targetType, consumer);
    }

    @Override
    public void filterStream(String objectName, ObjectFilterRequest request, int fetchSize, Consumer<Map<String, Object>> consumer) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Consumer<Map<String, Object>> mappedConsumer = row -> {
            mapFrom(objectMetadata, row);
            consumer.accept(row);
        };
        defaultQueryService.filterStream(
                objectMetadata,
                request,
                fetchSize,
                mappedConsumer
        );
    }

    public List<Map<String, Object>> findObject(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = defaultQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return pageResponse.getContents();
    }

    public Map<String, Object> findById(ActionContext<ObjectFilterRequest> request) {
        ObjectMetadata objectMetadata = getObjectMetadata(request.getResource());
        PageResponse<Map<String, Object>> pageResponse = defaultQueryService.filter(objectMetadata, request.getPayload());
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> mapFrom(objectMetadata, object));
        return Utils.CL.getFirst(pageResponse.getContents());
    }

    public Map<String, Object> save(String objectName, Map<String, Object> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        applyAudit(objectMetadata, AuditAction.CREATE, payload);
        mapTo(objectMetadata, payload);
        Map<String, Object> returnObject = defaultQueryService.insertReturning(objectMetadata, payload);
        mapFrom(objectMetadata, returnObject);
        changeLogRecorder.recordCreate(objectMetadata, nonNull(returnObject));
        return returnObject;
    }

    public List<Map<String, Object>> saveAll(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        payload.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = defaultQueryService.insertReturning(objectMetadata, payload);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));
        changeLogRecorder.recordCreate(objectMetadata, returnObjects);

        return returnObjects;
    }

    public int update(String objectName, Object id, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> oldData = defaultQueryService.findById(objectMetadata, id, null);
        if (oldData == null) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        RowChange change = updateData(objectMetadata, oldData, newData);
        int rows = defaultQueryService.updateById(objectMetadata, id, oldData);
        if (rows > 0 && change != null) {
            changeLogRecorder.recordUpdate(objectMetadata, List.of(change.before()), List.of(change.after()));
        }
        return rows;
    }

    public int[] updateAll(String objectName, List<Map<String, Object>> newDataList) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Attribute idAttribute = objectMetadata.getIdAttribute();
        String idFieldName = idAttribute.getFieldName();
        List<Object> ids = newDataList
                .stream()
                .map(oldData -> oldData.get(idFieldName))
                .collect(Collectors.toList());
        List<Map<String, Object>> oldDataList = defaultQueryService.findByIds(objectMetadata, ids);
        if (Utils.CL.isEmpty(oldDataList)) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }

        Map<Object, Map<String, Object>> oldDataListMapById = Utils.CL.toMap(oldDataList, dataKey -> dataKey.get(idFieldName), dataValue -> dataValue);

        Map<Map<String, Object>, RowChange> changes = new IdentityHashMap<>();
        newDataList.forEach(newObjectMap -> {
            Object id = newObjectMap.get(idFieldName);
            Map<String, Object> oldData = oldDataListMapById.get(idAttribute.cast(id));
            changes.put(oldData, updateData(objectMetadata, oldData, newObjectMap));
        });
        int[] counts = defaultQueryService.batchUpdate(objectMetadata, oldDataList);
        if (changeLogRecorder.isEnabled(objectMetadata)) {
            List<Map<String, Object>> befores = new ArrayList<>();
            List<Map<String, Object>> afters = new ArrayList<>();
            for (int i = 0; i < oldDataList.size(); i++) {
                RowChange change = changes.get(oldDataList.get(i));
                if (change != null && written(counts, i)) {
                    befores.add(change.before());
                    afters.add(change.after());
                }
            }
            changeLogRecorder.recordUpdate(objectMetadata, befores, afters);
        }
        return counts;
    }

    public Map<String, Object> updateReturning(String objectName, Object id, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> oldData = defaultQueryService.findById(objectMetadata, id, null);
        if (oldData == null) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        RowChange change = updateData(objectMetadata, oldData, newData);
        Map<String, Object> returnValueUpdated = defaultQueryService.updateByIdReturning(objectMetadata, id, oldData);
        mapFrom(objectMetadata, returnValueUpdated);
        if (change != null) {
            changeLogRecorder.recordUpdate(objectMetadata, List.of(change.before()), nonNull(returnValueUpdated));
        }
        return returnValueUpdated;
    }

    public List<Map<String, Object>> updateAllReturning(String objectName, List<Map<String, Object>> newDataList) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Attribute idAttribute = objectMetadata.getIdAttribute();
        String idFieldName = idAttribute.getFieldName();
        List<Object> ids = newDataList
                .stream()
                .map(oldData -> oldData.get(idFieldName))
                .collect(Collectors.toList());
        List<Map<String, Object>> oldDataList = defaultQueryService.findByIds(objectMetadata, ids);
        if (Utils.CL.isEmpty(oldDataList)) {
            throw ObjectErrors.notFound(objectMetadata.getName());
        }
        Map<Object, Map<String, Object>> oldDataListMapById = Utils.CL.toMap(oldDataList, dataKey -> dataKey.get(idFieldName), dataValue -> dataValue);

        List<Map<String, Object>> befores = new ArrayList<>();
        newDataList.forEach(newObjectMap -> {
            Object id = newObjectMap.get(idFieldName);
            Map<String, Object> oldData = oldDataListMapById.get(idAttribute.cast(id));
            RowChange change = updateData(objectMetadata, oldData, newObjectMap);
            if (change != null) {
                befores.add(change.before());
            }
        });

        List<Map<String, Object>> dataUpdatedList =  defaultQueryService.updateReturning(objectMetadata, oldDataList);

        Utils.CL.emptyIfNull(dataUpdatedList).forEach(dataUpdated -> mapFrom(objectMetadata, dataUpdated));
        if (!befores.isEmpty()) {
            changeLogRecorder.recordUpdate(objectMetadata, befores, Utils.CL.emptyIfNull(dataUpdatedList));
        }

        return dataUpdatedList;
    }

    /**
     * Merges {@code newData} into {@code oldData} (the row as read) and fills the update audit columns. With a change log,
     * returns copies of the row before and after the merge, in field-name form; otherwise {@code null}.
     */
    private RowChange updateData(ObjectMetadata objectMetadata, Map<String, Object> oldData, Map<String, Object> newData) {
        boolean logged = changeLogRecorder.isEnabled(objectMetadata);
        mapFrom(objectMetadata, oldData);
        Map<String, Object> before = logged ? changeLogDiff.copy(oldData) : null;
        try {
            Utils.O.updateValues(oldData, newData);
        } catch (JsonMappingException e) {
            throw ObjectErrors.payloadInvalidException(objectMetadata.getName(), e.getMessage(), e);
        }
        applyAudit(objectMetadata, AuditAction.UPDATE, oldData);
        Map<String, Object> after = logged ? changeLogDiff.copy(oldData) : null;
        mapTo(objectMetadata, oldData);
        return logged ? new RowChange(before, after) : null;
    }

    public int delete(String objectName, Object id, Map<String, Object> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        boolean soft = isSoftDeleted(objectMetadata);
        List<Map<String, Object>> befores = rowsBefore(objectMetadata, List.of(id));
        int rows;
        if (!soft) {
            rows = defaultQueryService.forceDeleteById(objectMetadata, id);
        } else {
            applyAudit(objectMetadata, AuditAction.DELETE, payload);
            rows = defaultQueryService.deleteById(objectMetadata, id, payload);
        }
        if (rows > 0) {
            changeLogRecorder.recordDelete(objectMetadata, befores, soft);
        }
        return rows;
    }

    public int delete(String objectName, Object id, Long version) {
        return delete(objectName, id, Utils.CL.newHashMap(Constants.VERSION, version));
    }

    public int delete(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Object> ids = payload.stream().map(map -> map.get(Constants.OBJECT_PK)).collect(Collectors.toList());
        boolean soft = isSoftDeleted(objectMetadata);
        List<Map<String, Object>> befores = rowsBefore(objectMetadata, ids);
        int rows;
        if(!soft) {
            rows = defaultQueryService.forceDeleteByIds(objectMetadata, ids);
        } else {
            Utils.CL.emptyIfNull(payload).forEach(objectMap -> applyAudit(objectMetadata, AuditAction.DELETE, objectMap));
            rows = defaultQueryService.delete(objectMetadata, payload);
        }
        recordDeleted(objectMetadata, befores, soft);
        return rows;
    }

    public int[] batchDelete(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Object> ids = payload.stream().map(map -> map.get(Constants.OBJECT_PK)).collect(Collectors.toList());
        boolean soft = isSoftDeleted(objectMetadata);
        List<Map<String, Object>> befores = rowsBefore(objectMetadata, ids);
        int[] rows;
        if(!soft) {
            rows = defaultQueryService.batchForceDeleteByIds(objectMetadata, ids);
        } else {
            Utils.CL.emptyIfNull(payload).forEach(objectMap -> applyAudit(objectMetadata, AuditAction.DELETE, objectMap));
            rows = defaultQueryService.batchDelete(objectMetadata, payload);
        }
        recordDeleted(objectMetadata, befores, soft);
        return rows;
    }

    public Map<String, Object> findById(String objectName, Object id, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = defaultQueryService.findById(objectMetadata, id, returnFields);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public Map<String, Object> findById(String objectName, Object id) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = defaultQueryService.findById(objectMetadata, id);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public List<Map<String, Object>> findByIds(String objectName, List<Object> ids) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        return findByIds(objectName, ids, objectMetadata.getFieldNames());
    }

    public List<Map<String, Object>> findByIds(String objectName, List<Object> ids, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> results = defaultQueryService.findByIds(objectMetadata, ids, returnFields);
        Utils.CL.emptyIfNull(results).forEach(object -> mapFrom(objectMetadata, object));
        return results;
    }

    public Map<String, Object> findOneByCondition(String objectName, Condition condition) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = defaultQueryService.findOneByCondition(objectMetadata, condition);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public Map<String, Object> findOneByCondition(String objectName, Condition condition, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        Map<String, Object> objectMap = defaultQueryService.findOneByCondition(objectMetadata, condition, returnFields);
        mapFrom(objectMetadata, objectMap);
        return objectMap;
    }

    public List<Map<String, Object>> findByCondition(String objectName, Condition condition) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> objectMap = defaultQueryService.findByCondition(objectMetadata, condition);
        Utils.CL.emptyIfNull(objectMap).forEach(object -> mapFrom(objectMetadata, object));
        return objectMap;
    }

    public List<Map<String, Object>> findByCondition(String objectName, Condition condition, List<String> returnFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> objectMap = defaultQueryService.findByCondition(objectMetadata, condition, returnFields);
        Utils.CL.emptyIfNull(objectMap).forEach(object -> mapFrom(objectMetadata, object));
        return objectMap;
    }

    public List<Map<String, Object>> bulkCreateIgnoreConflictOnConstraintName(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName) {

        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = defaultQueryService.insertReturningInsertedRows(objectMetadata, request, conflictOnConstraintName);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));
        changeLogRecorder.recordCreate(objectMetadata, returnObjects);

        return returnObjects;
    }

    public Map<String, Object> createIgnoreConflictOnConstraintName(String objectName, Map<String, Object> request, String conflictOnConstraintName) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        applyAudit(objectMetadata, AuditAction.CREATE, request);
        mapTo(objectMetadata, request);
        Map<String, Object> returnObject = defaultQueryService.insertReturningInsertedRow(objectMetadata, request, conflictOnConstraintName);
        mapFrom(objectMetadata, returnObject);
        changeLogRecorder.recordCreate(objectMetadata, nonNull(returnObject));
        return returnObject;
    }

    public List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> befores = changeLogRecorder.isEnabled(objectMetadata)
                ? rowsWithKeys(objectMetadata, request, constraintFields(objectMetadata, conflictOnConstraintName))
                : List.of();
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = defaultQueryService.upsertReturning(objectMetadata, request, conflictOnConstraintName);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));
        changeLogRecorder.recordUpdate(objectMetadata, befores, returnObjects);

        return returnObjects;
    }

    public List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, List<String> conflictFields) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> befores = changeLogRecorder.isEnabled(objectMetadata)
                ? rowsWithKeys(objectMetadata, request, attributes(objectMetadata, conflictFields))
                : List.of();
        request.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            mapTo(objectMetadata, objectMap);
        });

        List<Map<String, Object>> returnObjects = defaultQueryService.upsertReturning(objectMetadata, request, conflictFields);
        returnObjects.forEach(returnObject -> mapFrom(objectMetadata, returnObject));
        changeLogRecorder.recordUpdate(objectMetadata, befores, returnObjects);

        return returnObjects;
    }

    @Override
    public int upsert(String objectName, Map<String, Object> payload, List<String> conflictOnConstraintNames, Condition condition) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        boolean logged = changeLogRecorder.isEnabled(objectMetadata);
        List<Attribute> keys = logged ? attributes(objectMetadata, conflictOnConstraintNames) : List.of();
        Condition keyCondition = logged ? keyCondition(keys, List.of(payload)) : null;
        List<Map<String, Object>> befores = keyCondition == null ? List.of() : rowsMatching(objectMetadata, keyCondition);
        applyAudit(objectMetadata, AuditAction.CREATE, payload);
        mapTo(objectMetadata, payload);
        int rows = defaultQueryService.upsert(objectMetadata, payload, conflictOnConstraintNames, condition);
        if (rows > 0 && keyCondition != null) {
            changeLogRecorder.recordUpdate(objectMetadata, befores, rowsMatching(objectMetadata, keyCondition));
        }
        return rows;
    }

    @Override
    public int[] insertBatch(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> afters = prepareInsert(objectMetadata, payload);
        int[] counts = defaultQueryService.batchInsert(objectMetadata, payload);
        recordInserted(objectMetadata, afters, counts);
        return counts;
    }

    @Override
    public int updateWithCondition(String objectName, Condition condition, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        return updateLogged(objectMetadata, condition, () -> defaultQueryService.update(objectMetadata, newData, condition));
    }

    @Override
    public int updateWithExpressions(String objectName, Condition condition, Map<String, Object> newData) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        return updateLogged(objectMetadata, condition, () -> defaultQueryService.updateWithExpressions(objectMetadata, newData, condition));
    }

    @Override
    public List<Map<String, Object>> findByCondition(String objectName,
                                                     Condition condition,
                                                     int limit,
                                                     List<Sort> sortFields,
                                                     List<String> returnFields) {
        return defaultQueryService.findByCondition(getObjectMetadata(objectName), condition, limit, sortFields, returnFields);
    }

    @Override
    public int[] insertBatchIgnoreDuplicate(String objectName, List<Map<String, Object>> payload) {
        ObjectMetadata objectMetadata = getObjectMetadata(objectName);
        List<Map<String, Object>> afters = prepareInsert(objectMetadata, payload);
        int[] counts = defaultQueryService.insertBatchIgnoreDuplicate(objectMetadata, payload);
        recordInserted(objectMetadata, afters, counts);
        return counts;
    }

    /**
     * Fills the create audit columns and maps the rows for the insert. With a change log, gives each row without id a
     * new one (the insert returns nothing to read it back) and returns copies of the rows in field-name form.
     */
    private List<Map<String, Object>> prepareInsert(ObjectMetadata objectMetadata, List<Map<String, Object>> payload) {
        boolean logged = changeLogRecorder.isEnabled(objectMetadata);
        String idFieldName = objectMetadata.getIdAttribute().getFieldName();
        List<Map<String, Object>> afters = new ArrayList<>();
        payload.forEach(objectMap -> {
            applyAudit(objectMetadata, AuditAction.CREATE, objectMap);
            if (logged) {
                objectMap.putIfAbsent(idFieldName, UUID.randomUUID());
                afters.add(changeLogDiff.copy(objectMap));
            }
            mapTo(objectMetadata, objectMap);
        });
        return afters;
    }

    private void recordInserted(ObjectMetadata objectMetadata, List<Map<String, Object>> afters, int[] counts) {
        if (afters.isEmpty()) {
            return;
        }
        List<Map<String, Object>> inserted = new ArrayList<>();
        for (int i = 0; i < afters.size(); i++) {
            if (written(counts, i)) {
                inserted.add(afters.get(i));
            }
        }
        changeLogRecorder.recordCreate(objectMetadata, inserted);
    }

    private int updateLogged(ObjectMetadata objectMetadata, Condition condition, Supplier<Integer> update) {
        if (!changeLogRecorder.isEnabled(objectMetadata)) {
            return update.get();
        }
        List<Map<String, Object>> befores = rowsMatching(objectMetadata, condition);
        int rows = update.get();
        if (rows > 0 && !befores.isEmpty()) {
            changeLogRecorder.recordUpdate(objectMetadata, befores, rowsBefore(objectMetadata, ids(objectMetadata, befores)));
        }
        return rows;
    }

    /** Rows with the given ids, read for the change log; empty when the object has none. */
    private List<Map<String, Object>> rowsBefore(ObjectMetadata objectMetadata, List<Object> ids) {
        if (!changeLogRecorder.isEnabled(objectMetadata) || ids.isEmpty()) {
            return List.of();
        }
        List<Object> nonNullIds = ids.stream().filter(Objects::nonNull).toList();
        if (nonNullIds.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = changeLogQuery.findByIds(objectMetadata, nonNullIds);
        Utils.CL.emptyIfNull(rows).forEach(row -> mapFrom(objectMetadata, row));
        return Utils.CL.emptyIfNull(rows);
    }

    private List<Map<String, Object>> rowsMatching(ObjectMetadata objectMetadata, Condition condition) {
        List<Map<String, Object>> rows = changeLogQuery.findByCondition(objectMetadata, condition);
        Utils.CL.emptyIfNull(rows).forEach(row -> mapFrom(objectMetadata, row));
        return Utils.CL.emptyIfNull(rows);
    }

    /** Logs the rows of {@code befores} that a delete by ids removed (gone, or soft deleted, when read again). */
    private void recordDeleted(ObjectMetadata objectMetadata, List<Map<String, Object>> befores, boolean soft) {
        if (befores.isEmpty()) {
            return;
        }
        String idFieldName = objectMetadata.getIdAttribute().getFieldName();
        Set<String> kept = new HashSet<>();
        for (Map<String, Object> row : rowsBefore(objectMetadata, ids(objectMetadata, befores))) {
            if (!Boolean.TRUE.equals(row.get(Constants.IS_DELETED))) {
                kept.add(String.valueOf(row.get(idFieldName)));
            }
        }
        List<Map<String, Object>> deleted = befores.stream()
                .filter(row -> !kept.contains(String.valueOf(row.get(idFieldName))))
                .toList();
        changeLogRecorder.recordDelete(objectMetadata, deleted, soft);
    }

    /** Existing rows with the same key values as the payload rows (the rows an upsert would update). */
    private List<Map<String, Object>> rowsWithKeys(ObjectMetadata objectMetadata, List<Map<String, Object>> payload, List<Attribute> keys) {
        Condition condition = keyCondition(keys, payload);
        return condition == null ? List.of() : rowsMatching(objectMetadata, condition);
    }

    /** {@code (k1 = v1 and k2 = v2) or (…)} over the payload rows; rows with a null key value never conflict. */
    private Condition keyCondition(List<Attribute> keys, List<Map<String, Object>> payload) {
        if (keys.isEmpty()) {
            return null;
        }
        List<Condition> rows = new ArrayList<>();
        for (Map<String, Object> row : payload) {
            List<Condition> parts = new ArrayList<>();
            for (Attribute key : keys) {
                Object value = row.get(key.getFieldName());
                if (value == null) {
                    parts = null;
                    break;
                }
                Object keyValue = key.cast(value);
                parts.add(DSL.field(DSL.name(key.getColumnName())).eq(keyValue));
            }
            if (parts != null) {
                rows.add(DSL.and(parts));
            }
        }
        return rows.isEmpty() ? null : DSL.or(rows);
    }

    /**
     * Attributes of the columns of the constraint, for an object with a change log. Throws when they cannot be found:
     * without them the rows the upsert updates cannot be read first and would be logged as CREATE.
     */
    private List<Attribute> constraintFields(ObjectMetadata objectMetadata, String constraintName) {
        List<String> columns = uniqueConstraintColumns == null
                ? List.of()
                : uniqueConstraintColumns.columns(objectMetadata.getTableName(), constraintName);
        List<Attribute> attributes = attributes(objectMetadata, columns);
        if (attributes.isEmpty()) {
            throw new IllegalStateException("Change log of " + objectMetadata.getName() + ": no columns found for constraint "
                    + constraintName + " of table " + objectMetadata.getTableName() + " (columns " + columns
                    + "); the upsert is refused so that it is not logged as CREATE");
        }
        return attributes;
    }

    /** Attributes for field or column names; empty when one is unknown, so no key condition is built from a part. */
    private List<Attribute> attributes(ObjectMetadata objectMetadata, List<String> names) {
        Map<String, Attribute> byColumn = new HashMap<>();
        Utils.CL.emptyIfNull(objectMetadata.getAttributes()).forEach(attribute -> byColumn.put(attribute.getColumnName(), attribute));
        List<Attribute> attributes = new ArrayList<>();
        for (String name : Utils.CL.emptyIfNull(names)) {
            Attribute attribute = objectMetadata.getAttributeByName(name);
            if (attribute == null) {
                attribute = byColumn.get(name);
            }
            if (attribute == null) {
                return List.of();
            }
            attributes.add(attribute);
        }
        return attributes;
    }

    private List<Object> ids(ObjectMetadata objectMetadata, List<Map<String, Object>> rows) {
        String idFieldName = objectMetadata.getIdAttribute().getFieldName();
        return rows.stream().map(row -> row.get(idFieldName)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** JDBC batch count of row {@code i}: written when positive or SUCCESS_NO_INFO; counts missing: assumed written. */
    private boolean written(int[] counts, int i) {
        return counts == null || i >= counts.length || counts[i] > 0 || counts[i] == Statement.SUCCESS_NO_INFO;
    }

    private List<Map<String, Object>> nonNull(Map<String, Object> row) {
        return row == null ? List.of() : List.of(row);
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

    /** A row read before an update and the same row after it, in field-name form. */
    private record RowChange(Map<String, Object> before, Map<String, Object> after) {
    }
}

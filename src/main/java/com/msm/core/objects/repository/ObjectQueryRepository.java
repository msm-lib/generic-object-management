package com.msm.core.objects.repository;

import com.msm.core.action.context.ActionContext;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import org.jooq.Condition;

import java.util.List;
import java.util.Map;

public interface ObjectQueryRepository {
    PageResponse<Map<String, Object>> lookup(ActionContext<ObjectFilterRequest> request);

    PageResponse<Map<String, Object>> lookup(String objectName, ObjectFilterRequest request);

    PageResponse<Map<String, Object>> filter(ActionContext<ObjectFilterRequest> request);

    List<Map<String, Object>> findObject(ActionContext<ObjectFilterRequest> request);

    Map<String, Object> findById(ActionContext<ObjectFilterRequest> request);

    Map<String, Object> save(String objectName, Map<String, Object> payload);

    List<Map<String, Object>> save(String objectName, List<Map<String, Object>> payload);

    int update(String objectName, Object id, Map<String, Object> newData);

    int[] update(String objectName, List<Map<String, Object>> newDataList);

    Map<String, Object> updateReturning(String objectName, Object id, Map<String, Object> newData);

    List<Map<String, Object>> updateReturning(String objectName, List<Map<String, Object>> newDataList);

    int delete(String objectName, Object id, Map<String, Object> payload);

    int delete(String objectName, Object id, Long version);

    int delete(String objectName, List<Map<String, Object>> payload);

    int[] batchDelete(String objectName, List<Map<String, Object>> payload);

    Map<String, Object> findById(String objectName, Object id, List<String> returnFields);

    Map<String, Object> findById(String objectName, Object id);

    List<Map<String, Object>> findByIds(String objectName, List<Object> ids);

    List<Map<String, Object>> findByIds(String objectName, List<Object> ids, List<String> returnFields);

    Map<String, Object> findByCondition(String objectName, Condition condition);

    Map<String, Object> findByCondition(String objectName, Condition condition, List<String> returnFields);

    List<Map<String, Object>> findAllByCondition(String objectName, Condition condition);

    List<Map<String, Object>> findAllByCondition(String objectName, Condition condition, List<String> returnFields);

    List<Map<String, Object>> bulkCreateIgnoreConflictOnConstraintName(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName);

    Map<String, Object> createIgnoreConflictOnConstraintName(String objectName, Map<String, Object> request, String conflictOnConstraintName);

    List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, String conflictOnConstraintName);

    List<Map<String, Object>> bulkUpsertReturning(String objectName, List<Map<String, Object>> request, List<String> conflictFields);
}

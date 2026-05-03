package com.msm.core.objects.service;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.hook.common.ActionExecutor;
import com.msm.core.hook.context.ActionContext;
import com.msm.core.objects.dto.ObjectConversionRequest;
import com.msm.core.objects.dto.QueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class GenericObjectService {

    private final ActionExecutor actionExecutor;

    public PageResponse<Object> filter(ObjectFilterRequest filter) {
        ActionContext<ObjectFilterRequest> actionContext = ActionContext
                .<ObjectFilterRequest>builder()
                .resource(filter.getObjectInfo().getName())
                .action(Constants.GENERIC_FILTER_ACTION)
                .payload(filter)
                .build();
        return actionExecutor.execute(actionContext);
    }

    public Object getObjectById(String objectName, UUID id, List<String> returnFields) {
        FilterGroup filterGroup = new FilterGroup();
        filterGroup.setOperator(LogicalOperator.AND);
        FilterCondition filterCondition = FilterCondition.create(Constants.OBJECT_PK, FilterOperator.EQUALS, id);
        filterGroup.setConditions(Utils.CL.newArrayList(filterCondition));
        ObjectFilterRequest objectFilter = ObjectFilterRequest
                .builder()
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .filters(filterGroup)
                .build();
        objectFilter.setReturnFields(returnFields);

        ActionContext<ObjectFilterRequest> actionRequest = ActionContext
                .<ObjectFilterRequest>builder()
                .resource(objectName)
                .action(Constants.GENERIC_FILTER_BY_ID_ACTION)
                .payload(objectFilter)
                .build();

        return actionExecutor.execute(actionRequest);
    }

    public List<Object> getAllObject(String objectName, List<String> returnFields) {
        ObjectFilterRequest objectFilter = ObjectFilterRequest
                .builder()
                .returnFields(returnFields)
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .build();
        ActionContext<ObjectFilterRequest> actionRequest = ActionContext
                .<ObjectFilterRequest>builder()
                .resource(objectName)
                .action(Constants.GENERIC_ALL_OBJECT_ACTION)
                .payload(objectFilter)
                .build();

        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Object createObject(String objectName, Map<String, Object> request) {

        ActionContext<Map<String, Object>> actionRequest = ActionContext
                .<Map<String, Object>>builder()
                .resource(objectName)
                .action(Constants.Action.CREATE)
                .payload(request)
                .build();

        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public List<Object> createObjects(String objectName, List<Map<String, Object>> request) {
        return request.stream().map(objectMap -> createObject(objectName, objectMap)).toList();
    }

    @Transactional
    public Object updateObject(String objectName, UUID id, Map<String, Object> request) {
        ActionContext<Map<String, Object>> actionRequest = ActionContext
                .<Map<String, Object>>builder()
                .resource(objectName)
                .objectId(id)
                .action(Constants.Action.UPDATE)
                .payload(request)
                .build();
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public void deleteObject(String objectName, UUID id) {
        ActionContext<Map<String, Object>> actionRequest = ActionContext
                .<Map<String, Object>>builder()
                .resource(objectName)
                .objectId(id)
                .action(Constants.Action.DELETE)
                .payload(Utils.CL.newHashMap())
                .build();
        actionExecutor.execute(actionRequest);
    }

    @Transactional
    public void deleteObject(String objectName, List<UUID> ids) {
        ids.forEach(id -> deleteObject(objectName, id));
    }

    @Transactional
    public Object conversion(ObjectConversionRequest request) {
        ActionContext<ObjectConversionRequest> actionRequest = ActionContext
                .<ObjectConversionRequest>builder()
                .resource(request.getTargetObject())
                .action(Constants.OBJECT_CONVERSION_ACTION)
                .payload(request)
                .build();

        return actionExecutor.execute(actionRequest);
    }

    public Object queryTemplate(QueryTemplate request) {
        ActionContext<QueryTemplate> actionRequest = ActionContext
                .<QueryTemplate>builder()
                .resource(Constants.RESOURCE_QUERY_PREFIX)
                .action(request.getQuery())
                .payload(request)
                .build();
        return actionExecutor.execute(actionRequest);
    }
}

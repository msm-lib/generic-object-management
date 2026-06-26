package com.msm.core.objects.service.internal;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.objects.dto.ObjectConversionRequest;
import com.msm.core.objects.dto.ObjectDeleteRequest;
import com.msm.core.objects.dto.QueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class InternalGenericObjectService {

    private final ActionExecutor actionExecutor;

    public PageResponse<Object> lookup(ObjectFilterRequest filter) {

        ActionContext<ObjectFilterRequest> actionContext = createActionContext(filter.getObjectInfo().getName(), Constants.FilterAction.LOOKUP_OBJECT, filter);
        return actionExecutor.execute(actionContext);
    }

    public PageResponse<Object> filter(ObjectFilterRequest filter) {

        ActionContext<ObjectFilterRequest> actionContext = createActionContext(filter.getObjectInfo().getName(), Constants.FilterAction.FILTER_OBJECT, filter);
        return actionExecutor.execute(actionContext);
    }

    public Map<String, Object> getObjectById(String objectName, UUID id, List<String> returnFields) {
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

        ActionContext<ObjectFilterRequest> actionRequest = createActionContext(objectName, Constants.FilterAction.FILTER_OBJECT_BY_ID, objectFilter);
        return actionExecutor.execute(actionRequest);
    }

    public List<Map<String, Object>> getAllObject(String objectName, List<String> returnFields) {

        ObjectFilterRequest objectFilter = ObjectFilterRequest
                .builder()
                .returnFields(returnFields)
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .build();
        ActionContext<ObjectFilterRequest> actionRequest = createActionContext(objectName, Constants.FilterAction.FILTER_ALL_OBJECT, objectFilter);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Map<String, Object> createObject(String objectName, Map<String, Object> request) {

        ActionContext<Map<String, Object>> actionRequest = createActionContext(objectName, Constants.Action.CREATE, request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public List<Map<String, Object>> bulkCreateObject(String objectName, List<Map<String, Object>> request) {

        ActionContext<List<Map<String, Object>>> actionRequest = createActionContext(objectName, Constants.Action.BULK_CREATE, request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Map<String, Object> updateObject(String objectName, UUID id, Map<String, Object> request) {
        ActionContext<Map<String, Object>> actionRequest = createActionContext(objectName, id, Constants.Action.UPDATE, request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public List<Map<String, Object>> bulkUpdateObject(String objectName, List<Map<String, Object>> request) {
        ActionContext<List<Map<String, Object>>> actionRequest = createActionContext(objectName, Constants.Action.BULK_UPDATE, request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Map<String, Object> objectAction(
            String objectName,
            UUID id,
            String actionName,
            Map<String, Object> request) {

        ActionContext<Map<String, Object>> actionRequest = createActionContext(objectName, id, actionName, Utils.O.defaultIfNull(request, Utils.CL::newHashMap));
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public List<Map<String, Object>> objectActionBulkUpdate(
            String objectName,
            String actionName,
            List<Map<String, Object>> request) {

        ActionContext<List<Map<String, Object>>> actionRequest = createActionContext(objectName, actionName, Utils.O.defaultIfNull(request, Utils.CL::newArrayList));
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public void deleteObject(String objectName, UUID id, Long version) {
        ActionContext<Map<String, Object>> actionRequest = createActionContext(objectName, id, Constants.Action.DELETE, Utils.CL.newHashMap(Constants.VERSION, version));
        actionExecutor.execute(actionRequest);
    }

    @Transactional
    public void deleteObjects(String objectName, List<ObjectDeleteRequest> params) {
        ActionContext<List<Map<String, Object>>> actionRequest = createActionContext(objectName, Constants.Action.BULK_DELETE, params.stream().map(Utils.O::<String, Object>toMap).collect(Collectors.toList()));
        actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Object conversion(ObjectConversionRequest request) {
        ActionContext<ObjectConversionRequest> actionRequest = createActionContext(request.getTargetObject(), Constants.OBJECT_CONVERSION_ACTION, request);
        return actionExecutor.execute(actionRequest);
    }

    public Object queryTemplate(QueryTemplate request) {
        ActionContext<QueryTemplate> actionRequest = createActionContext(Constants.RESOURCE_QUERY_PREFIX, request.getQuery(), request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public List<Map<String, Object>> importFileByFileId(String objectName, Map<String, Object> request) {
        ActionContext<Map<String, Object>> actionRequest = createActionContext(objectName, Constants.Action.IMPORT_OBJECT, request);
        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Map<String, Object> importFile(String objectName, MultipartFile file) {
        ActionContext<MultipartFile> actionRequest = createActionContext(objectName, Constants.Action.IMPORT_FILE_OBJECT, file);
        return actionExecutor.execute(actionRequest);
    }

    private <X> ActionContext<X> createActionContext(String objectName, String action, X payload) {
        return ActionContext
                .<X>builder()
                .resource(objectName)
                .action(action)
                .payload(payload)
                .internalExecution(true)
                .build();
    }

    private <X> ActionContext<X> createActionContext(String objectName, Object objectId, String action, X payload) {
        ActionContext<X> xActionContext = createActionContext(objectName, action, payload);
        xActionContext.setObjectId(objectId);
        return xActionContext;
    }
}

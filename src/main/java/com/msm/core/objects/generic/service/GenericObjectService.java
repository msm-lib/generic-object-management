package com.msm.core.objects.generic.service;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.domain.*;
import com.msm.core.hook.common.ActionExecutor;
import com.msm.core.hook.context.ActionRequest;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.exception.ServiceErrorEnum;
import com.msm.core.objects.generic.ObjectConstants;
import com.msm.core.objects.generic.dto.ObjectConversionRequest;
import com.msm.core.objects.generic.dto.QueryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings({"unchecked"})
@Slf4j
//@Service
@RequiredArgsConstructor
public class GenericObjectService {

    private final AdvancedFilterService advancedFilterService;
    private final ActionExecutor actionExecutor;
    private final DefaultSoftDeleteFilter defaultSoftDeleteFilter;
    private final DynamicQueryService dynamicQueryService;

    public PageResponse<Object> filter(ObjectFilterRequest filter) {
        ActionRequest<ObjectFilterRequest> actionRequest = ActionRequest
                .<ObjectFilterRequest>builder()
                .objectName(filter.getObjectInfo().getName())
                .action(Constants.GENERIC_FILTER_ACTION)
                .disableHookEvent(true)
                .payload(filter)
                .build();
//        return dynamicQueryService.query(filter);
        return actionExecutor.execute(actionRequest);
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
//        defaultSoftDeleteFilter.addDefaultFilter(objectFilter);
//        List<Object> objects = advancedFilterService.filter(objectFilter).getContents();
//        if(Utils.CL.isEmpty(objects)) {
//            throw Errors.throwException(ServiceErrorEnum.NOT_FOUND, id);
//        }
//        Object result = objects.getFirst();
//        preprocessObject(result);


        ActionRequest<ObjectFilterRequest> actionRequest = ActionRequest
                .<ObjectFilterRequest>builder()
                .objectName(objectName)
                .action(Constants.GENERIC_FILTER_BY_ID_ACTION)
                .disableHookEvent(true)
                .payload(objectFilter)
                .build();

        return actionExecutor.execute(actionRequest);

//        return result;
    }

    public List<Object> getAllObject(String objectName, List<String> returnFields) {
        ObjectFilterRequest objectFilter = ObjectFilterRequest
                .builder()
                .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(Utils.CL.newArrayList(defaultSoftDeleteFilter.defaultFilterGroup())).build())
                .returnFields(returnFields)
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .build();
        ActionRequest<ObjectFilterRequest> actionRequest = ActionRequest
                .<ObjectFilterRequest>builder()
                .objectName(objectName)
                .action(Constants.GENERIC_ALL_OBJECT_ACTION)
                .disableHookEvent(true)
                .payload(objectFilter)
                .build();

        return actionExecutor.execute(actionRequest);
    }

    @Transactional
    public Object createObject(String objectName, Map<String, Object> request) {

        ActionRequest<Map<String, Object>> actionRequest = ActionRequest
                .<Map<String, Object>>builder()
                .objectName(objectName)
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
        ActionRequest<Map<String, Object>> actionRequest = ActionRequest
                .<Map<String, Object>>builder()
                .objectName(objectName)
                .objectId(id)
                .action(Constants.Action.UPDATE)
                .payload(request)
                .build();
        Object object = actionExecutor.execute(actionRequest);
        Map<String, Object> result = Utils.O.toMap(object);
        preprocessObject(result);
        return result;
    }

    @Transactional
    public void deleteObject(String objectName, UUID id) {
        ActionRequest<Map<String, Object>> actionRequest = ActionRequest
                .<Map<String, Object>>builder()
                .objectName(objectName)
                .objectId(id)
                .action(Constants.Action.DELETE)
                .payload(Map.of("id", id))
                .build();
        actionExecutor.execute(actionRequest);
    }

    @Transactional
    public void deleteObject(String objectName, List<UUID> ids) {
        ids.forEach(id -> deleteObject(objectName, id));
    }

    @Transactional
    public Object conversion(ObjectConversionRequest request) {
        ActionRequest<Map<String, Object>> actionRequest = ActionRequest
                .<Map<String, Object>>builder()
                .additionalParameter(request.getSourceObject())
                .objectName(request.getTargetObject())
                .action(Constants.OBJECT_CONVERSION_ACTION)
                .payload(request.getSrcData())
                .build();
        return actionExecutor.execute(actionRequest);
    }

    private void preprocessObject(Object object) {
        if(Map.class.isAssignableFrom(object.getClass())) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            ActionRequest<Map<String, Object>> request = ActionRequest.<Map<String, Object>>builder()
                    .action(ObjectConstants.UNWRAPPED_CUSTOM_VALUES)
                    .payload(objectMap)
                    .disableHookEvent(true)
                    .build();
            actionExecutor.execute(request);
        }
    }

    public Object queryTemplate(QueryTemplate request) {
        ActionRequest<QueryTemplate> actionRequest = ActionRequest
                .<QueryTemplate>builder()
                .action(request.getQuery())
                .payload(request)
                .build();
        return actionExecutor.execute(actionRequest);
    }
}

package com.msm.core.objects.handler;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class GenericObjectHandler {
    private final ObjectQueryRepository genericObjectQueryRepository;
    private final ObjectQueryRepository internalObjectQueryRepository;

    @Handler(action = Constants.FilterAction.LOOKUP_OBJECT)
    public PageResponse<Map<String, Object>> lookupHandler(ActionContext<ObjectFilterRequest> actionContext) {
        return getGenericObjectQueryRepository(actionContext).lookup(actionContext);
    }

    @Handler(action = Constants.FilterAction.FILTER_OBJECT)
    public PageResponse<Map<String, Object>> filterHandler(ActionContext<ObjectFilterRequest> actionContext) {
        return getGenericObjectQueryRepository(actionContext).filter(actionContext);
    }

    @Handler(action = Constants.FilterAction.FILTER_ALL_OBJECT)
    public List<Map<String, Object>> findObjectHandler(ActionContext<ObjectFilterRequest> actionContext) {
        return getGenericObjectQueryRepository(actionContext).findObject(actionContext);
    }

    @Handler(action = Constants.FilterAction.FILTER_OBJECT_BY_ID)
    public Map<String, Object> findByIdHandler(ActionContext<ObjectFilterRequest> actionContext) {
        return getGenericObjectQueryRepository(actionContext).findById(actionContext);
    }

    @Handler(action = Constants.Action.CREATE)
    public Map<String, Object> createHandler(ActionContext<Map<String, Object>> actionContext) {
        createCodeIfNull(actionContext.getResource(), actionContext.getPayload());
        return getGenericObjectQueryRepository(actionContext)
                .save(actionContext.getResource(), actionContext.getPayload());
    }

    @Handler(action = Constants.Action.BULK_CREATE)
    public List<Map<String, Object>> bulkCreateHandler(ActionContext<List<Map<String, Object>>> actionContext) {
        createCodeIfNull(actionContext.getResource(), actionContext.getPayload());
        return getGenericObjectQueryRepository(actionContext).save(actionContext.getResource(), actionContext.getPayload());
    }

    @Handler(action = Constants.Action.UPDATE)
    public Map<String, Object> updateHandler(ActionContext<Map<String, Object>> actionContext) {
        return getGenericObjectQueryRepository(actionContext)
                .updateReturning(actionContext.getResource(), actionContext.getObjectId(), actionContext.getPayload());
    }

    @Handler(action = Constants.Action.BULK_UPDATE)
    public List<Map<String, Object>> bulkUpdateHandler(ActionContext<List<Map<String, Object>>> actionContext) {
        return getGenericObjectQueryRepository(actionContext).updateReturning(actionContext.getResource(), actionContext.getPayload());
    }

    @Handler(action = Constants.Action.DELETE)
    public int deleteHandler(ActionContext<Map<String, Object>> actionContext) {
        return getGenericObjectQueryRepository(actionContext).delete(actionContext.getResource(), actionContext.getObjectId(), actionContext.getPayload());
    }

    @Handler(action = Constants.Action.BULK_DELETE)
    public int bulkDeleteHandler(ActionContext<List<Map<String, Object>>> actionContext) {
        return getGenericObjectQueryRepository(actionContext).delete(actionContext.getResource(), actionContext.getPayload());
    }

    public void createCodeIfNull(String objectName, Map<String, Object> payload) {
        Object code = payload.get("code");
        if(Objects.isNull(code)) {
            String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(objectName)), () -> "");
            int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
            payload.put("code", Utils.toCodeGenerator(prefix, len));
        }
    }

    public void createCodeIfNull(String objectName, List<Map<String, Object>> payloads) {
        payloads.forEach(objectMap -> {
            Object code = objectMap.get("code");
            if(Objects.isNull(code)) {
                String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(objectName)), () -> "");
                int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
                objectMap.put("code", Utils.toCodeGenerator(prefix, len));
            }
        });
    }

    public ObjectQueryRepository getGenericObjectQueryRepository(ActionContext<?> actionContext) {
        if(actionContext.isInternalExecution()) {
            return internalObjectQueryRepository;
        }
        return genericObjectQueryRepository;
    }
}

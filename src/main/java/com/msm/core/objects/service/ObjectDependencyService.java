package com.msm.core.objects.service;

import com.msm.core.action.context.ActionContext;

import java.util.Map;

public interface ObjectDependencyService {

    void saveObjectDependency(ActionContext<Map<String, Object>> actionContext);
    void sendEvent(ActionContext<Map<String, Object>> actionContext);

}
package com.msm.core.objects.service;

import com.msm.core.hook.context.ActionContext;

import java.util.Map;

public interface ObjectUsageService {

    void saveObjectUsage(ActionContext<Map<String, Object>> actionContext);
    void sendEvent(ActionContext<Map<String, Object>> actionContext);

}
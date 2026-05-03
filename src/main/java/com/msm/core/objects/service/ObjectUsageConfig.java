package com.msm.core.objects.service;

import com.msm.core.hook.context.ActionContext;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ObjectUsageConfig{

    private final GenericObjectMetadataService genericObjectMetadataService;

    public void saveObjectUsage(ActionContext<Map<String, Object>> actionContext) {

    }

    public void sendEvent(ActionContext<Map<String, Object>> actionContext) {

    }
}

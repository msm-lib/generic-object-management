package com.msm.core.objects.security;

import com.msm.core.security.context.RequestContext;

import java.util.Map;

public interface SecurityFieldResolver {
    String supportObjectType();
    Map<String, Object> resolve(
            String objectName,
            Object source,
            RequestContext context
    );
}

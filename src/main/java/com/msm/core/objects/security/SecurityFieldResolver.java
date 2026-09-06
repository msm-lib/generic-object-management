package com.msm.core.objects.security;

import com.msm.core.security.context.RequestContext;
import com.msm.core.strategy.TypedStrategy;

import java.util.Map;

public interface SecurityFieldResolver extends TypedStrategy<String> {
    Map<String, Object> resolve(
            String objectName,
            Object source,
            RequestContext context
    );
}

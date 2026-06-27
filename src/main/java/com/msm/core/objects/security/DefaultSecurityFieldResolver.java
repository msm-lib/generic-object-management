package com.msm.core.objects.security;

import com.msm.core.objects.dto.ObjectConversionRequest;
import com.msm.core.security.context.RequestContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultSecurityFieldResolver implements SecurityFieldResolver {

    @Override
    public String supportObjectType() {
        return "default";
    }

    @Override
    public Map<String, Object> resolve(
            String objectName,
            Object source,
            RequestContext context) {

        ObjectConversionRequest request = (ObjectConversionRequest) source;
        return request.getSrcData();
    }
}

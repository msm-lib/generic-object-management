package com.msm.core.objects.security;

import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.security.context.RequestContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultSecurityFieldResolver implements SecurityFieldResolver {

    @Override
    public String supportObjectType() {

        return "";
    }

    @Override
    public Map<String, Object> resolve(
            String objectName,
            Object source,
            RequestContext context) {

        ObjectMetadata meta = ObjectMetadataFactory.getObjectMetadataByName(objectName);

        Map<String, Object> result = new HashMap<>();


        return result;
    }
}

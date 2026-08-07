package com.msm.core.objects.security;

public abstract class AbstractConversionSecurityFieldResolver implements SecurityFieldResolver {

    @Override
    public String supportObjectType() {

        return ObjectSecurityUtils.buildKey(sourceObject(), targetObject());
    }

    abstract public String sourceObject();
    abstract public String targetObject();
}

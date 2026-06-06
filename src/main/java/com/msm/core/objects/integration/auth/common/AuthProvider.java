package com.msm.core.objects.integration.auth.common;

import com.msm.core.objects.integration.context.HttpRequestContext;

public interface AuthProvider {
    String providerName();
    void apply(HttpRequestContext httpRequestContext);
}

package com.msm.core.objects.integration.auth.bearer;

import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;

public class StaticTokenProvider implements TokenProvider {

    @Override
    public String getToken(HttpRequestContext httpRequestContext) {
        return "test123";
    }
}
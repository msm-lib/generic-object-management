package com.msm.core.objects.integration.auth.apikey;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiKeyAuthProvider implements AuthProvider {

    private final String headerName;
    private final String apiKey;

    @Override
    public void apply(HttpRequestContext httpRequestContext) {
        httpRequestContext.getHeaders().set(headerName, apiKey);
    }
}
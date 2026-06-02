package com.msm.core.objects.integration.auth.basic;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.BasicCredentials;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class BasicAuthenticationProvider implements AuthProvider {

    private final BasicCredentials credentials;

    @Override
    public void apply(HttpRequestContext httpRequestContext) {
        httpRequestContext.getHeaders().setBasicAuth(credentials.getUsername(), credentials.getPassword());
    }
}

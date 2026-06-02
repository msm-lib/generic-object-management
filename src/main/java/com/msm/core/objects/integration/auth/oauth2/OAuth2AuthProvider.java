package com.msm.core.objects.integration.auth.oauth2;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuth2AuthProvider implements AuthProvider {

    private final TokenProvider tokenProvider;

    @Override
    public void apply(HttpRequestContext httpRequestContext) {
        httpRequestContext.getHeaders().setBearerAuth(tokenProvider.getToken(httpRequestContext));
    }
}

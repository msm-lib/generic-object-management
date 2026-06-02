package com.msm.core.objects.integration.auth.oauth2password;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuth2PasswordProvider implements AuthProvider {
    private final static String PARAM_TOKEN_NAME = "token";
    private final TokenProvider oAuth2TokenManager;

    @Override
    public void apply(HttpRequestContext httpRequestContext) {
        httpRequestContext.getHeaders().add(PARAM_TOKEN_NAME, oAuth2TokenManager.getToken(httpRequestContext));
    }
}
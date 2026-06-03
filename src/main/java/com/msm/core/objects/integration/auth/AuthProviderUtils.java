package com.msm.core.objects.integration.auth;

import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.apikey.ApiKeyAuthProvider;
import com.msm.core.objects.integration.auth.basic.BasicAuthenticationProvider;
import com.msm.core.objects.integration.auth.bearer.BearerAuthProvider;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.auth.oauth2.CachedOAuth2TokenManager;
import com.msm.core.objects.integration.auth.oauth2.OAuth2AuthProvider;
import com.msm.core.objects.integration.data.AuthProviderProperties;
import com.msm.core.objects.integration.data.ConnectorProperties;
import com.msm.core.objects.integration.retry.RetryExecutor;

public class AuthProviderUtils {
    public static AuthProvider createProvider(ConnectorProperties config, RequestClient restClient, RetryExecutor retryExecutor) {

        AuthProviderProperties authProviderProperties =  config.getAuth();
        return switch (authProviderProperties.getType()) {

            case "basic" -> new BasicAuthenticationProvider(authProviderProperties.getBasic());
            case "bearer" -> new BearerAuthProvider(httpRequestContext -> authProviderProperties.getToken());

            case "apikey" -> new ApiKeyAuthProvider(
                    authProviderProperties.getApikey().getName(),
                    authProviderProperties.getApikey().getValue()
            );

            case "oauth2" -> createOAuth2Provider(
                    authProviderProperties,
                    restClient,
                    retryExecutor
            );

            default -> throw new IllegalArgumentException("Unsupported auth type: " + authProviderProperties.getType());
        };
    }

    private static AuthProvider createOAuth2Provider(AuthProviderProperties config, RequestClient restClient, RetryExecutor retryExecutor) {
        TokenProvider tokenManager = new CachedOAuth2TokenManager(restClient, retryExecutor, config.getOauth2());
        return new OAuth2AuthProvider(tokenManager);
    }
}

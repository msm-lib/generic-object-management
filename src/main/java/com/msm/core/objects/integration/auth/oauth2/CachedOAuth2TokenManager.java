package com.msm.core.objects.integration.auth.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.outh2.OAuth2Properties;
import com.msm.core.objects.integration.data.outh2.OAuth2Token;
import com.msm.core.objects.integration.data.outh2.OAuth2TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CachedOAuth2TokenManager implements TokenProvider {
    private final RequestClient requestClient;
    private final OAuth2Properties properties;
    private volatile OAuth2Token token;

    @Override
    public synchronized String getToken(HttpRequestContext httpRequestContext) {
        if (token == null || token.isExpired(properties.getSkewSeconds())) {
            token = fetchToken();
        }
        return token.getAccessToken();
    }

    private OAuth2Token fetchToken() {

        Map<String, String> formBody = Map.of(
                "grant_type",
                    properties.getGrantType(),
                    "client_id",
                    properties.getClientId(),
                    "client_secret",
                    properties.getClientSecret(),
                    "scope",
                    properties.getScope()
        );

        OAuth2TokenResponse response = requestClient.post(
                properties.getTokenUrl(), "",
                properties.isStrictJsonMode() ? toJson(formBody) : formBody,
                OAuth2TokenResponse.class);

        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(response.getAccessToken());
        token.setExpiresIn(response.getExpiresIn());
        token.setCreatedAt(Instant.now());

        return token;
    }

    private String toJson(Map<String, String> formBody) {
        try {
            return Utils.O.toJsonString(formBody);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
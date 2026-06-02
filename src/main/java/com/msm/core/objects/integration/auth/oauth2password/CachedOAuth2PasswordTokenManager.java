package com.msm.core.objects.integration.auth.oauth2password;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.JwtUtils;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.outh2.OAuth2PasswordProperties;
import com.msm.core.objects.integration.data.outh2.OAuth2PasswordTokenResponse;
import com.msm.core.objects.integration.data.outh2.OAuth2Token;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import com.msm.core.objects.integration.retry.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CachedOAuth2PasswordTokenManager implements TokenProvider {
    private final RequestClient requestClient;
    private final RetryExecutor retryExecutor;
    private final OAuth2PasswordProperties properties;
    private volatile OAuth2Token token;


    @Override
    public synchronized String getToken(HttpRequestContext httpRequestContext) {
        if (token == null || token.isExpired(properties.getSkewSeconds())) {
            HttpRequestContext authContext = createAuthContext(httpRequestContext.getConnectorName(), properties);
            try {
                token = retryExecutor.execute(authContext, this::fetchToken);
            } catch (Exception ex) {
                httpRequestContext.addEvents(authContext.getEvents());
                throw ex;
            }
        }
        return token.getAccessToken();
    }

    private OAuth2Token fetchToken() {
        Map<String, String> formBody = new HashMap<>();
//        formBody.put("grant_type", "password");
        formBody.put("username", properties.getUsername());
        formBody.put("password", properties.getPassword());
        formBody.put("scope", properties.getScope());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        OAuth2PasswordTokenResponse response = requestClient.post(
                properties.getTokenUrl(),
                "",
                headers,
                properties.isStrictJsonMode() ? toJson(formBody) : formBody,
                OAuth2PasswordTokenResponse.class);

        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(response.getAccessToken());
        token.setCreatedAt(Instant.now());
        token.setExpiresIn(JwtUtils.getRemainingTimeMs(response.getAccessToken()));
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

    private HttpRequestContext createAuthContext(String connectorName, OAuth2PasswordProperties properties) {
        RetryRequestConfig config = new RetryRequestConfig();
        config.setMaxAttempts(properties.getMaxAttempts());
        config.setWaitDuration(Duration.ofMillis(properties.getWaitDurationMs()));
        return HttpRequestContext.builder()
                .connectorName(connectorName)
                .componentExecution(getClass().getSimpleName() + ".authenticate")
                .retryConfig(config)
                .build();
    }
}
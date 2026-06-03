package com.msm.core.objects.integration.auth.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.objects.integration.IntegrationJsonUtil;
import com.msm.core.objects.integration.IntegrationTokenCache;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.JwtUtils;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.auth.enums.OAuth2GrantType;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.outh2.OAuth2Properties;
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
public class CachedOAuth2TokenManager implements TokenProvider {
    private final RequestClient requestClient;
    private final RetryExecutor retryExecutor;
    private final OAuth2Properties properties;

    @Override
    public String getToken(HttpRequestContext httpRequestContext) {
        OAuth2Token auth2CacheToken = IntegrationTokenCache.getOrCompute(properties.getTokenUrl(), () -> retryFetchToken(httpRequestContext));
        if (auth2CacheToken == null || auth2CacheToken.isExpired(properties.getSkewSeconds())) {
            auth2CacheToken = retryFetchToken(httpRequestContext);
            IntegrationTokenCache.put(properties.getTokenUrl(), auth2CacheToken);
        }
        return auth2CacheToken.getAccessToken();
    }

    private OAuth2Token retryFetchToken(HttpRequestContext httpRequestContext) {
        HttpRequestContext authContext = createAuthContext(httpRequestContext.getConnectorName(), properties);
        try {
            return retryExecutor.execute(authContext, this::fetchToken);
        } catch (Exception ex) {
            httpRequestContext.addEvents(authContext.getEvents());
            throw ex;
        }
    }

    private OAuth2Token fetchToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Map<String, String> formBody = buildBody(properties);
        Object response = requestClient.post(
                properties.getTokenUrl(),
                "",
                headers,
                properties.isStrictJsonMode() ? toJson(formBody) : formBody,
                Object.class);

        String accessToken = parseToken(response);
        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(accessToken);
        token.setCreatedAt(Instant.now());
        token.setExpiresIn(JwtUtils.getRemainingTimeMs(accessToken));

        return token;
    }

    private String parseToken(Object response) {
        Map<String, Object> objectMap = Utils.O.toMap(response);
        String accessTokenName = Utils.STR.defaultIfBlank(properties.getAccessTokenPath(), () -> "token");
        return IntegrationJsonUtil.extractValue(objectMap, accessTokenName);
    }

    private Map<String, String> buildBody(OAuth2Properties properties) {
        Map<String, String> formBody = new HashMap<>();
        if(OAuth2GrantType.CLIENT_CREDENTIALS.equals(properties.getGrantType())) {
            formBody.put("grant_type", properties.getGrantType().getValue());
            formBody.put("client_id", properties.getClientId());
            formBody.put("client_secret", properties.getClientSecret());
            formBody.put("scope", properties.getScope());
        } else {
            formBody.put("username", properties.getCredential().getUsername());
            formBody.put("password", properties.getCredential().getPassword());
            formBody.put("scope", properties.getScope());
        }
        return formBody;
    }

    private String toJson(Map<String, String> formBody) {
        try {
            return Utils.O.toJsonString(formBody);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private HttpRequestContext createAuthContext(String connectorName, OAuth2Properties properties) {
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
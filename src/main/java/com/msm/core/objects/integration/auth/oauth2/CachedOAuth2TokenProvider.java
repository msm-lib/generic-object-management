package com.msm.core.objects.integration.auth.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.objects.cache.RedisCacheOperator;
import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.integration.IntegrationJsonUtil;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.JwtUtils;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.ConnectorProperties;
import com.msm.core.objects.integration.data.outh2.OAuth2Properties;
import com.msm.core.objects.integration.data.outh2.OAuth2Token;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import com.msm.core.objects.integration.retry.RetryExecutor;
import com.msm.core.security.RequestContextHolder;
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
public class CachedOAuth2TokenProvider implements TokenProvider {
    private final RequestClient requestClient;
    private final RetryExecutor retryExecutor;
    private final IntegrationProperties integrationProperties;
    private final RedisCacheOperator redisCacheOperator;

    @Override
    public String supportProvider() {
        return "oauth2-credentials";
    }

    @Override
    public String getToken(HttpRequestContext ctx) {
        OAuth2Properties oAuth2Context = getOAuth2Properties(ctx);
        String keyCache = cacheKey(oAuth2Context);
        OAuth2Token auth2CacheToken = redisCacheOperator.get(keyCache, OAuth2Token.class);
        if (auth2CacheToken == null || JwtUtils.isTokenExpired(oAuth2Context.getAccessTokenPath()) || ctx.isForceReNewToken()) {
            auth2CacheToken = retryFetchToken(ctx, oAuth2Context);
            redisCacheOperator.set(keyCache, auth2CacheToken);
        }

        return auth2CacheToken.getAccessToken();
    }

    private OAuth2Token retryFetchToken(HttpRequestContext ctx, OAuth2Properties oAuth2Context) {
        HttpRequestContext authRequestContext = createAuthContext(ctx.getConnectorName(), oAuth2Context);
        try {
            return retryExecutor.execute(authRequestContext, () -> fetchToken(oAuth2Context));
        } catch (Exception ex) {
            ctx.addEvents(authRequestContext.getEvents());
            throw ex;
        }
    }

    private OAuth2Token fetchToken(OAuth2Properties oAuth2Context) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Map<String, String> formBody = buildBody(oAuth2Context);
        Object response = requestClient.post(
                oAuth2Context.getTokenUrl(),
                "",
                headers,
                oAuth2Context.isStrictJsonMode() ? toJson(formBody) : formBody,
                Object.class);

        String accessToken = parseToken(response, oAuth2Context);
        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(accessToken);
        token.setCreatedAt(Instant.now());
        token.setExpiresIn(JwtUtils.getRemainingTimeMs(accessToken));

        return token;
    }

    private String parseToken(Object response, OAuth2Properties oAuth2Context) {
        Map<String, Object> objectMap = Utils.O.toMap(response);
        String accessTokenName = Utils.STR.defaultIfBlank(oAuth2Context.getAccessTokenPath(), () -> "token");
        return IntegrationJsonUtil.extractValue(objectMap, accessTokenName);
    }

    private Map<String, String> buildBody(OAuth2Properties properties) {
        Map<String, String> formBody = new HashMap<>();
        formBody.put("grant_type", properties.getGrantType());
        formBody.put("client_id", properties.getClientId());
        formBody.put("client_secret", properties.getClientSecret());
        formBody.put("scope", properties.getScope());
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

    private OAuth2Properties getOAuth2Properties(HttpRequestContext ctx) {
        ConnectorProperties connectorProperties = integrationProperties.getConnectors().get(ctx.getConnectorName());
        return Utils.O.convertObject(connectorProperties.getAuth().getProperties(), OAuth2Properties.class);
    }

    private String cacheKey(OAuth2Properties oAuth2Context) {
        return RequestContextHolder.getRequestContext().getTenantCode() + ":" + supportProvider() + ":" + oAuth2Context.getTokenUrl();
    }
}
package com.msm.core.objects.integration;

import com.msm.core.commons.Utils;
import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.ConnectorProperties;
import com.msm.core.objects.integration.data.retry.RetryRequestBuilder;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ConnectorRequestBuilder {

    private final String connectorName;

    private final IntegrationProperties integrationProperties;

    private final IntegrationClientExchange integrationClientExchange;

    private RetryRequestConfig retryConfig;

    private final HttpHeaders headers = new HttpHeaders();

    private final Map<String, Object> queryParams = new HashMap<>();

    private Integer retryAttempts;

    private String authProvider;
    private Boolean strictJsonMode;

    public ConnectorRequestBuilder strictJsonMode(Boolean strictJsonMode) {
        this.strictJsonMode = strictJsonMode;
        return this;
    }

    public ConnectorRequestBuilder retry(int maxAttempts) {
        RetryRequestConfig config = new RetryRequestConfig();
        config.setMaxAttempts(maxAttempts);
        this.retryConfig = config;

        return this;
    }

    public ConnectorRequestBuilder header(String name, String value) {
        headers.add(name, value);
        return this;
    }

    public ConnectorRequestBuilder header(MultiValueMap<String, String> multiValueMap) {
        headers.addAll(multiValueMap);
        return this;
    }

    public ConnectorRequestBuilder queryParam(String key, Object value) {
        queryParams.put(key, value);
        return this;
    }

    public ConnectorRequestBuilder queryParam(Map<String, Object> params) {
        queryParams.putAll(params);
        return this;
    }

    public ConnectorRequestBuilder retry(Consumer<RetryRequestBuilder> consumer) {
        RetryRequestBuilder builder = new RetryRequestBuilder();
        consumer.accept(builder);
        this.retryConfig = builder.build();

        return this;
    }

    public <T> T get(String path, Class<T> responseType) {

        HttpRequestContext context = createContext(HttpMethod.GET, path, null);

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public <T> T get(String path, ParameterizedTypeReference<T> responseType) {

        HttpRequestContext context = createContext(HttpMethod.GET, path, null);

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public <T> T post(
            String path,
            Object body,
            Class<T> responseType) {

        HttpRequestContext context = createContext(
                HttpMethod.POST,
                path,
                body
        );

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public <T> T post(
            String path,
            Object body,
            ParameterizedTypeReference<T> responseType) {

        HttpRequestContext context = createContext(
                HttpMethod.POST,
                path,
                body
        );

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public <T> T put(String path, Object body, Class<T> responseType) {

        HttpRequestContext context = createContext(
                HttpMethod.PUT,
                path,
                body
        );

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public <T> T put(String path, Object body, ParameterizedTypeReference<T> responseType) {

        HttpRequestContext context = createContext(
                HttpMethod.PUT,
                path,
                body
        );

        return integrationClientExchange.exchange(
                context,
                responseType
        );
    }

    public void delete(String path) {

        HttpRequestContext context = createContext(
                HttpMethod.DELETE,
                path,
                null
        );

        integrationClientExchange.exchange(
                context,
                Void.class
        );
    }

    private HttpRequestContext createContext(
            HttpMethod method,
            String path,
            Object body) {
        ConnectorProperties connector = integrationProperties.getConnectors().get(connectorName);
        return HttpRequestContext.builder()
                .connectorName(connectorName)
                .strictJsonMode(Utils.O.defaultIfNull(strictJsonMode, () -> Boolean.FALSE))
                .baseUrl(connector.getBaseUrl())
                .path(path)
                .method(method)
                .headers(headers)
                .queryParams(queryParams)
                .body(body)
                .authConfig(connector.getAuth())
                .authProvider(
                        authProvider != null
                                ? authProvider
                                : connector.getAuth().getProvider()
                )
                .retryAttempts(
                        retryAttempts != null
                                ? retryAttempts
                                : connector.getRetry().getMaxAttempts()
                )
                .retryConfig(retryConfig)
                .build();
    }
}

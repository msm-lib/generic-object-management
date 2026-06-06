package com.msm.core.objects.integration.context;

import com.msm.core.commons.Utils;
import com.msm.core.objects.entity.integration.IntegrationLog;
import com.msm.core.objects.integration.data.AuthProviderProperties;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@ToString
public class HttpRequestContext {

//    private String traceId;
//
//    private String integrationName;
//
//    private HttpMethod method;
//
//    private String url;
//
//    private HttpHeaders headers;
//
//    private Object requestBody;
//
//    private Object responseBody;
//
//    private Integer statusCode;
//
//    private Throwable error;
//
//    private Instant startTime;
//
//    private Instant endTime;
//
//    @Builder.Default
//    private Map<String, Object> attributes =
//            new HashMap<>();

    private String connectorName;
    private String authProvider;

    private AuthProviderProperties authConfig;

    private String baseUrl;

    private String path;

    private HttpMethod method;

    @Builder.Default
    private HttpHeaders headers = new HttpHeaders();

    @Builder.Default
    private Map<String, Object> queryParams = new HashMap<>();

    private Object body;

//    private String url;

    private Integer retryAttempts;

    private RetryRequestConfig retryConfig;

    private Boolean strictJsonMode;

//    private HttpRequest httpRequest;

    public String resolveUrl() {
        return Utils.STR.defaultIfBlank(baseUrl, () -> "") + Utils.STR.defaultIfBlank(path, () -> "");
    }


    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) attributes.get(key);
    }

    private IntegrationLog integrationLog;

    private final List<ExecutionEvent> events = new ArrayList<>();

    public void addEvent(ExecutionEvent event) {
        events.add(event);
    }

    public void addEvents(List<ExecutionEvent> newEvents) {
        events.addAll(newEvents);
    }

    private String componentExecution;
    private Integer statusCode;
}
package com.msm.core.objects.connector.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class InternalHttpClient {

    private final RestClient restClient;

    public <T> T get(String baseUrl, String path, Class<T> responseType) {

        return exchange(
                baseUrl,
                path,
                HttpMethod.GET,
                null,
                null,
                responseType
        );
    }
    public <T> T get(String baseUrl, String path, Map<String, Object> queryParams, Class<T> responseType) {

        return exchange(
                baseUrl,
                path,
                HttpMethod.GET,
                queryParams,
                null,
                responseType
        );
    }

    public <T> T post(String baseUrl, String path, Object body, Class<T> responseType) {

        return exchange(
                baseUrl,
                path,
                HttpMethod.POST,
                null,
                body,
                responseType
        );
    }

    public <T> T put(String baseUrl, String path, Object body, Class<T> responseType) {

        return exchange(
                baseUrl,
                path,
                HttpMethod.PUT,
                null,
                body,
                responseType
        );
    }

    public void delete(String baseUrl, String path) {

        exchange(
                baseUrl,
                path,
                HttpMethod.DELETE,
                null,
                null,
                Void.class
        );
    }


    private <T> T exchange(String baseUrl, String path, HttpMethod method, Map<String, Object> queryParams, Object body, Class<T> responseType) {

        if (baseUrl == null) {
            throw new RuntimeException("Unknown baseUrl");
        }

        String url = baseUrl + path;
        log.info("[INTERNAL] {} {}", method, url);

        try {

            URI uri = buildUri(url, queryParams);

            RestClient.RequestBodySpec request =
                    restClient.method(method).uri(uri);

            if (body != null) {
                request.body(body);
            }

            return request
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {

                                String error = new String(
                                        res.getBody().readAllBytes()
                                );

                                log.error("[INTERNAL-ERROR] {}", error);

                                throw new RuntimeException(error);
                            }
                    )
                    .body(responseType);

        } catch (Exception ex) {
            log.error(
                    "[INTERNAL-EXCEPTION] baseUrl={} path={} error={}",
                    baseUrl,
                    path,
                    ex.getMessage()
            );
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Object> errorParams(String baseUrl, String path, Map<String, Object> queryParams, Object body) {
        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", baseUrl);
        result.put("path", path);
        result.put("queryParams", queryParams);
        result.put("body", body);
        return result;
    }

    private URI buildUri(String url, Map<String, Object> queryParams) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (queryParams != null) {
            queryParams.forEach((key, value) -> appendQueryParam(builder, key, value));
        }

        return builder.build(true).toUri();
    }

    private void appendQueryParam(UriComponentsBuilder builder, String key, Object value) {

        if (value == null) return;
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> builder.queryParam(key, item));
            return;
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object item : array) {
                builder.queryParam(key, item);
            }
            return;
        }
        builder.queryParam(key, value);
    }
}
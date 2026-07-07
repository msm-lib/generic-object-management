package com.msm.core.objects.integration;

import com.msm.core.commons.Utils;
import com.msm.core.objects.exception.integration.IntegrationRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class DefaultRequestClient implements RequestClient {
    private final RestClient restClient;

    @Override
    public <T> T get(String baseUrl, String path, Class<T> responseType) {
        return get(baseUrl, path, new HttpHeaders(), null, responseType);
    }

    @Override
    public <T> T get(String baseUrl, String path, HttpHeaders headers, Class<T> responseType) {
        return get(baseUrl, path, headers, null, responseType);
    }

    @Override
    public <T> T get(String baseUrl, String path, Map<String, Object> queryParams, Class<T> responseType) {
        return get(baseUrl, path, new HttpHeaders(), queryParams, responseType);
    }

    @Override
    public <T> T get(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Class<T> responseType) {
        return exchange(
                baseUrl,
                path,
                HttpMethod.GET,
                headers,
                queryParams,
                null,
                responseType
        );
    }

    @Override
    public <T> T post(String baseUrl, String path, Object body, Class<T> responseType) {
        return post(baseUrl, path, new HttpHeaders(), null, body, responseType);
    }

    @Override
    public <T> T post(String baseUrl, String path, HttpHeaders headers, Object body, Class<T> responseType) {
        return post(baseUrl, path, headers, null, body, responseType);
    }

    @Override
    public <T> T post(String baseUrl, String path, Map<String, Object> queryParams, Object body, Class<T> responseType) {
        return post(baseUrl, path, new HttpHeaders(), queryParams, body, responseType);
    }

    @Override
    public <T> T post(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Object body, Class<T> responseType) {
        return exchange(
                baseUrl,
                path,
                HttpMethod.POST,
                headers,
                queryParams,
                body,
                responseType
        );
    }

    @Override
    public <T> T put(String baseUrl, String path, Object body, Class<T> responseType) {
        return put(baseUrl, path, new HttpHeaders(), null, body, responseType);
    }

    @Override
    public <T> T put(String baseUrl, String path, HttpHeaders headers, Object body, Class<T> responseType) {
        return put(baseUrl, path, headers, null, body, responseType);
    }

    @Override
    public <T> T put(String baseUrl, String path, Map<String, Object> queryParams, Object body, Class<T> responseType) {
        return put(baseUrl, path, new HttpHeaders(), queryParams, body, responseType);
    }

    @Override
    public <T> T put(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Object body, Class<T> responseType) {
        return exchange(
                baseUrl,
                path,
                HttpMethod.PUT,
                headers,
                queryParams,
                body,
                responseType
        );
    }

    @Override
    public void delete(String baseUrl, String path, HttpHeaders headers) {
        delete(baseUrl, path, headers, null);
    }

    @Override
    public void delete(String baseUrl, String path, Map<String, Object> queryParams) {
        delete(baseUrl, path, new HttpHeaders(), queryParams);
    }

    @Override
    public void delete(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams) {
        exchange(
                baseUrl,
                path,
                HttpMethod.DELETE,
                headers,
                queryParams,
                null,
                Void.class
        );
    }


    public <T> T exchange(
            String baseUrl,
            String path,
            HttpMethod method,
            HttpHeaders headers,
            Map<String, Object> queryParams,
            Object body,
            Class<T> responseType) {

        String url = Utils.STR.defaultIfBlank(baseUrl, () -> "") + Utils.STR.defaultIfBlank(path, () -> "");
        if (Utils.STR.isEmpty(url)) {
            throw new RuntimeException("Unknown baseUrl");
        }
        log.info("[INTERNAL] {} {}", method, url);

        try {
            URI uri = buildUri(baseUrl, path, queryParams);
            RestClient.RequestBodySpec request = restClient
                    .method(method).uri(uri)
                    .headers(h -> h.addAll(headers));
            if (Objects.nonNull(body)) {
                request.body(body);
            }

            return request
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {
                                String bodyError = new String(res.getBody().readAllBytes());
                                log.error("[HTTP ERROR] URL: {} STATUS: {} BODY: {} ",
                                        req.getURI(),
                                        res.getStatusCode(),
                                        bodyError
                                );
                                throw new IntegrationRequestException(res.getStatusCode().value(), res.getStatusCode().toString(), bodyError);
                            }
                    )
                    .body(responseType);

        } catch (Exception ex) {
            log.error("[INTERNAL-EXCEPTION] baseUrl={} path={}", baseUrl, path, ex);
            throw ex;
        }
    }

    public <T> T exchange(
            String baseUrl,
            String path,
            HttpMethod method,
            HttpHeaders headers,
            Map<String, Object> queryParams,
            Object body,
            ParameterizedTypeReference<T> responseType) {

        String url = Utils.STR.defaultIfBlank(baseUrl, () -> "") + Utils.STR.defaultIfBlank(path, () -> "");
        if (Utils.STR.isEmpty(url)) {
            throw new RuntimeException("Unknown baseUrl");
        }

        log.info("[INTERNAL] {} {}", method, url);

        try {

            URI uri = buildUri(baseUrl, path, queryParams);
            RestClient.RequestBodySpec request = restClient.method(method).uri(uri).headers(h -> h.addAll(headers));

            if (Objects.nonNull(body)) {
                request.body(body);
            }

            return request
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {
                                String bodyError = new String(res.getBody().readAllBytes());
                                log.error("[HTTP ERROR] URL: {} STATUS: {} BODY: {} ",
                                        req.getURI(),
                                        res.getStatusCode(),
                                        bodyError
                                );
                                throw new IntegrationRequestException(res.getStatusCode().value(), res.getStatusCode().toString(), bodyError);
                            }
                    )
                    .body(responseType);

        } catch (Exception ex) {
            log.error("[INTERNAL-EXCEPTION] baseUrl={} path={}", baseUrl, path, ex);
            throw ex;
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

    private URI buildUri(String baseUrl, String path, Map<String, Object> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(Utils.STR.defaultIfBlank(baseUrl, () -> ""))
                .path(Utils.STR.defaultIfBlank(path, () -> ""));
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

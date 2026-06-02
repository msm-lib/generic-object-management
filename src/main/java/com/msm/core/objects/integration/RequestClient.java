package com.msm.core.objects.integration;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Map;

public interface RequestClient {
    <T> T get(String baseUrl, String path, Class<T> responseType);
    <T> T get(String baseUrl, String path, HttpHeaders headers, Class<T> responseType);
    <T> T get(String baseUrl, String path, Map<String, Object> queryParams, Class<T> responseType);
    <T> T get(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Class<T> responseType);

    <T> T post(String baseUrl, String path, Object body, Class<T> responseType);
    <T> T post(String baseUrl, String path, HttpHeaders headers, Object body, Class<T> responseType);
    <T> T post(String baseUrl, String path, Map<String, Object> queryParams, Object body, Class<T> responseType);
    <T> T post(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Object body, Class<T> responseType);


    <T> T put(String baseUrl, String path, Object body, Class<T> responseType);
    <T> T put(String baseUrl, String path, HttpHeaders headers, Object body, Class<T> responseType);
    <T> T put(String baseUrl, String path, Map<String, Object> queryParams, Object body, Class<T> responseType);
    <T> T put(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams, Object body, Class<T> responseType);

    void delete(String baseUrl, String path, HttpHeaders headers);
    void delete(String baseUrl, String path, Map<String, Object> queryParams);
    void delete(String baseUrl, String path, HttpHeaders headers, Map<String, Object> queryParams);

    <T> T exchange(
            String baseUrl,
            String path,
            HttpMethod method,
            HttpHeaders headers,
            Map<String, Object> queryParams,
            Object body,
            Class<T> responseType);

    <T> T exchange(
            String baseUrl,
            String path,
            HttpMethod method,
            HttpHeaders headers,
            Map<String, Object> queryParams,
            Object body,
            ParameterizedTypeReference<T> responseType);
}

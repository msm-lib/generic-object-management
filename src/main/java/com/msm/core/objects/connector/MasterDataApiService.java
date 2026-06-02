package com.msm.core.objects.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MasterDataApiService {

    private final RestClient internalRestClient;

    public void post(List<Map<String, Object>> requests) {
        internalRestClient
                .post()
                .uri("http://localhost:8090/master-data/api/v1/cn/portal/generic/objects/objectdependency/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requests)
                .retrieve()
                .body(String.class);
    }
}
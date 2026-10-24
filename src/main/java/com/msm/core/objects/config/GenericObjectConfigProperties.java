package com.msm.core.objects.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "objects")
public class GenericObjectConfigProperties {
    private Executor executor = new Executor();
    private ApiConfig apiConfig = new ApiConfig();

    @Data
    public static class Executor {
        private int core = 5;
        private int max = 20;
    }

    @Data
    public static class ApiConfig {
        /** Base URL of every service without an entry in {@link #serviceUrls}; the service path "/{service}" is appended. */
        private String baseUrl;
        /**
         * Internal host per service (the value of USER_SERVICE_URL, ORDER_SERVICE_URL…, without the service path), keyed by
         * the service name of {@link com.msm.core.objects.dataexchange.imports.ObjectServiceFactory}: user, customer, order,
         * inventory, master-data, catalog. "/{service}" is appended as for {@link #baseUrl}; a blank entry falls back to it.
         */
        private Map<String, String> serviceUrls = new HashMap<>();
    }
}

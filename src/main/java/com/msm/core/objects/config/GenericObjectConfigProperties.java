package com.msm.core.objects.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "objects")
public class GenericObjectConfigProperties {
    private Executor executor = new Executor();
    private ApiConfig apiConfig = new ApiConfig();

    @Data
    public static class Executor {
        private int core = 1;
        private int max = 20;
    }

    //dev, qc, uat
    @Data
    public static class ApiConfig {
        private String baseUrl;
    }
}

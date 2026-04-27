package com.msm.core.objects.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "msm")
public class GenericObjectConfigProperties {
    private Executor executor = new Executor();

    @Data
    public static class Executor {
        private int core = 1;
        private int max = 20;
    }
}

package com.msm.core.objects.config;

import com.msm.core.objects.integration.data.ConnectorProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "integration")
@Data
public class IntegrationProperties {
    private Map<String, ConnectorProperties> connectors = new HashMap<>();
}
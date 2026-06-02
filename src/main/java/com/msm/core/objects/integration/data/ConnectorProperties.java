package com.msm.core.objects.integration.data;

import com.msm.core.objects.integration.data.retry.RetryProperties;
import lombok.Data;

@Data
public class ConnectorProperties {
    private String baseUrl;
    private AuthProviderProperties auth;
    private RetryProperties retry = new RetryProperties();
}

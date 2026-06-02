package com.msm.core.objects.integration.factory;

import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.AuthProviderUtils;
import com.msm.core.objects.integration.retry.RetryExecutor;

public class AuthProviderRegistryFactory {

    public AuthProviderRegistry create(RequestClient requestClient, RetryExecutor retryExecutor, IntegrationProperties properties) {

        AuthProviderRegistry registry = new AuthProviderRegistry();
        properties.getConnectors()
                .forEach((name, config) -> {
                    String key = name + "." + config.getAuth().getType();
                    registry.register(key, AuthProviderUtils.createProvider(config, requestClient, retryExecutor));
                });

        return registry;
    }
}

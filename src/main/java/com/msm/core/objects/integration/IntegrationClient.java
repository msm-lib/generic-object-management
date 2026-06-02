package com.msm.core.objects.integration;

import com.msm.core.objects.config.IntegrationProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IntegrationClient {

    private final IntegrationProperties integrationProperties;
    private final IntegrationClientExchange integrationClientExchange;

    public ConnectorRequestBuilder connector(String connectorName) {

        return new ConnectorRequestBuilder(
                connectorName,
                integrationProperties,
                integrationClientExchange
        );
    }
}

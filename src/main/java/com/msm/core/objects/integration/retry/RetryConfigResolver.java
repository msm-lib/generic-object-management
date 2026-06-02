package com.msm.core.objects.integration.retry;

import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.ConnectorProperties;
import com.msm.core.objects.integration.data.retry.RetryDefaultProperties;
import com.msm.core.objects.integration.data.retry.RetryProperties;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import com.msm.core.objects.integration.exception.IntegrationException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class RetryConfigResolver {

    private final IntegrationProperties properties;

    private final RetryDefaultProperties defaults;

    public Retry resolve(String connectorName) {

        ConnectorProperties connector = properties.getConnectors().get(connectorName);

        int maxAttempts = connector.getRetry() != null &&
                connector.getRetry().getMaxAttempts() != null
                ? connector.getRetry().getMaxAttempts()
                : defaults.getMaxAttempts();

        RetryConfig config = RetryConfig
                .custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(defaults.getWaitDurationMs()))
                .build();
        return Retry.of(connectorName + "-retry", config);
    }


    public Retry resolve(HttpRequestContext context) {

        ConnectorProperties connector = properties.getConnectors().get(context.getConnectorName());
        RetryProperties retryProperties = connector.getRetry();
        RetryRequestConfig override = context.getRetryConfig();

        int maxAttempts = override != null && override.getMaxAttempts() != null
                ? override.getMaxAttempts()
                : retryProperties.getMaxAttempts();

        Duration waitDuration = override != null && override.getWaitDuration() != null
                ? override.getWaitDuration()
                : Duration.ofMillis(retryProperties.getWaitDurationMs());

        RetryConfig config = RetryConfig
                .custom()
                .maxAttempts(maxAttempts)
                .waitDuration(waitDuration)
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        NullPointerException.class
                )
                .retryOnException(shouldRetryPredicate())
                .build();


        return Retry.of(context.getConnectorName() + "-retry", config);
    }

    private Predicate<Throwable> shouldRetryPredicate() {
        return throwable -> {
            if (throwable instanceof IntegrationException ex) {
                return checkStatusCode(ex.getStatusCode());
            }
            if (throwable instanceof HttpClientErrorException ex) {
                return checkStatusCode(ex.getStatusCode().value());
            }
            return true;
        };
    }

    private boolean checkStatusCode(int statusCode) {
        if (statusCode == 429) {
            return true;
        }
        return statusCode < 400 || statusCode > 500;
    }
}

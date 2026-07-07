package com.msm.core.objects.integration.retry;

import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.exception.integration.IntegrationException;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.data.ConnectorProperties;
import com.msm.core.objects.integration.data.retry.RetryDefaultProperties;
import com.msm.core.objects.integration.data.retry.RetryProperties;
import com.msm.core.objects.integration.data.retry.RetryRequestConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class RetryConfigResolver {

    private final IntegrationProperties properties;

    private final RetryDefaultProperties defaults;


    private RetryConfig retryConfig(HttpRequestContext context, Predicate<Throwable> predicateRetry, List<Class<? extends Throwable>> ignoreExceptions) {
        ConnectorProperties connector = properties.getConnectors().get(context.getConnectorName());
        RetryProperties retryProperties = connector.getRetry();
        RetryRequestConfig override = context.getRetryConfig();
        Class<? extends Throwable>[] ignoreExceptionsArray = ignoreExceptions.toArray(Class[]::new);


        int maxAttempts = override != null && override.getMaxAttempts() != null
                ? override.getMaxAttempts()
                : retryProperties.getMaxAttempts();

        Duration waitDuration = override != null && override.getWaitDuration() != null
                ? override.getWaitDuration()
                : Duration.ofMillis(retryProperties.getWaitDurationMs());

        return RetryConfig
                .custom()
                .maxAttempts(maxAttempts)
                .waitDuration(waitDuration)
                .ignoreExceptions(ignoreExceptionsArray)
                .retryOnException(predicateRetry)
                .build();
    }

    public Retry resolve(HttpRequestContext context) {
        RetryConfig config = retryConfig(context, shouldRetryPredicate(), List.of(IllegalArgumentException.class, NullPointerException.class));
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




    public Retry resolveExchangeRetry(HttpRequestContext context) {
        RetryConfig config = retryConfig(context, shouldExchangeRetryPredicate(), List.of(IllegalArgumentException.class, NullPointerException.class));
        return Retry.of(context.getConnectorName() + "-exchangeRetry", config);
    }


    private Predicate<Throwable> shouldExchangeRetryPredicate() {
        return throwable -> {
            if (throwable instanceof IntegrationException ex) {
                return checkStatusCodeExchangeRetry(ex.getStatusCode());
            }
            if (throwable instanceof HttpClientErrorException ex) {
                return checkStatusCodeExchangeRetry(ex.getStatusCode().value());
            }
            return true;
        };
    }

    private boolean checkStatusCodeExchangeRetry(int statusCode) {
        return statusCode == 401;
    }
}

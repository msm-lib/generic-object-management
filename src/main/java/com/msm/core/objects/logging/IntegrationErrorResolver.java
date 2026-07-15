package com.msm.core.objects.logging;

/**
 * SPI that maps a {@link Throwable} thrown from an {@link IntegrationLogging}-annotated method onto
 * the failure fields of an {@link IntegrationLogData} (status code, error code/message, error
 * details). Consuming services contribute one or more {@code @Component} implementations; the aspect
 * injects them ordered by {@link org.springframework.core.annotation.Order} and applies the first
 * whose {@link #supports(Throwable)} returns {@code true}. This keeps knowledge of service-specific
 * exception types out of the reusable aspect.
 */
public interface IntegrationErrorResolver {

    boolean supports(Throwable ex);

    void resolve(Throwable ex, IntegrationLogData.IntegrationLogDataBuilder builder);
}

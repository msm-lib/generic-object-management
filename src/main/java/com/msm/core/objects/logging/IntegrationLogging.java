package com.msm.core.objects.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method whose invocations must be recorded as an integration_log row
 * (request/response payload, duration, tenant, and success/error outcome).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationLogging {
    String connector();

    String operation();
}

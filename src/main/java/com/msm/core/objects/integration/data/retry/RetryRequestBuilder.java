package com.msm.core.objects.integration.data.retry;

import java.time.Duration;

public class RetryRequestBuilder {

    private final RetryRequestConfig config =
            new RetryRequestConfig();

    public RetryRequestBuilder maxAttempts(
            int maxAttempts) {

        config.setMaxAttempts(maxAttempts);
        return this;
    }

    public RetryRequestBuilder waitDuration(Duration waitDuration) {

        config.setWaitDuration(waitDuration);
        return this;
    }

    public RetryRequestBuilder multiplier(
            double multiplier) {

        config.setMultiplier(multiplier);
        return this;
    }

    public RetryRequestBuilder exponentialBackoff(
            boolean enabled) {

        config.setExponentialBackoff(enabled);
        return this;
    }

    public RetryRequestConfig build() {
        return config;
    }
}

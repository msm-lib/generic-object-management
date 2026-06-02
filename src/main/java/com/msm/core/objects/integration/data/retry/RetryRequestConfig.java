package com.msm.core.objects.integration.data.retry;

import lombok.Data;

import java.time.Duration;

@Data
public class RetryRequestConfig {

    private Integer maxAttempts;

    private Duration waitDuration;

    private Double multiplier;

    private Boolean exponentialBackoff;
}

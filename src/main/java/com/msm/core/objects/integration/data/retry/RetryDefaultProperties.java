package com.msm.core.objects.integration.data.retry;

import lombok.Data;

@Data
public class RetryDefaultProperties {
    private int maxAttempts = 3;
    private long waitDurationMs = 200;
}

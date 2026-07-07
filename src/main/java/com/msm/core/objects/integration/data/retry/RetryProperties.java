package com.msm.core.objects.integration.data.retry;

import lombok.Data;

@Data
public class RetryProperties {
    private int maxAttempts = 1;
    private long waitDurationMs = 200L;
}

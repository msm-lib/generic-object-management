package com.msm.core.objects.integration.data.retry;

import lombok.Data;

@Data
public class RetryProperties {
    private Integer maxAttempts;

    private Long waitDurationMs;

    public int resolveMaxAttempts(int defaultValue) {
        return maxAttempts != null ? maxAttempts : defaultValue;
    }
}

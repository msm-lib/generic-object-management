package com.msm.core.objects.exception.integration;

import com.msm.core.objects.exception.ObjectErrorCode;

public class RateLimitException
        extends IntegrationException {

    public RateLimitException() {
        super(ObjectErrorCode.RATE_LIMIT, "RATE_LIMIT", 429, "Rate limit exceeded");
    }
}

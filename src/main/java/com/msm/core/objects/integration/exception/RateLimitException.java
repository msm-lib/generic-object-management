package com.msm.core.objects.integration.exception;

import com.msm.core.objects.exception.ObjectErrorCode;

public class RateLimitException
        extends IntegrationException {

    public RateLimitException() {
        super(ObjectErrorCode.RATE_LIMIT, "RATE_LIMIT", 429, "Rate limit exceeded");
    }
}

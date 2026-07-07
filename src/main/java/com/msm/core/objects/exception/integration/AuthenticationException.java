package com.msm.core.objects.exception.integration;

import com.msm.core.objects.exception.ObjectErrorCode;

public class AuthenticationException extends IntegrationException {

    public AuthenticationException() {
        super(ObjectErrorCode.AUTH_ERROR, "AUTH_ERROR", 401, "Authentication failed");
    }
}

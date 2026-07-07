package com.msm.core.objects.exception.integration;

import com.msm.core.objects.exception.ObjectErrorCode;

public class IntegrationRequestException extends IntegrationException {
    public IntegrationRequestException(Integer statusCode, String errorCode, String message) {
        super(ObjectErrorCode.INTEGRATION_REQUEST_ERROR, errorCode, statusCode, message);
    }
}

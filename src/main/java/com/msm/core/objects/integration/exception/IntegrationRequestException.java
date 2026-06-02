package com.msm.core.objects.integration.exception;

import com.msm.core.objects.exception.ObjectErrorCode;

public class IntegrationRequestException extends IntegrationException {
    public IntegrationRequestException(Integer statusCode, String errorCode, String message) {
        super(ObjectErrorCode.INTEGRATION_REQUEST_ERROR, errorCode, statusCode, message);
    }
}

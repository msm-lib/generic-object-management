package com.msm.core.objects.integration.exception;

import com.msm.core.exceptions.common.ErrorCode;
import lombok.Getter;

@Getter
public class IntegrationException extends RuntimeException {

    private final ErrorCode code;
    private final String errorCode;
    private final Integer statusCode;

    public IntegrationException(ErrorCode code, String errorCode, Integer statusCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
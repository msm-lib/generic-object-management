package com.msm.core.objects.exception;

import com.msm.core.exceptions.GenericBaseException;

import java.util.Map;

public class InternalObjectClientException extends GenericBaseException {
    private static final String DEFAULT_MESSAGE = "Internal client call api error";

    public InternalObjectClientException(Map<String, Object> objectErrors, Throwable cause) {
        super(ObjectErrorCode.INTERNAL_OBJECT_CLIENT, DEFAULT_MESSAGE, objectErrors, cause);
    }

    public InternalObjectClientException(Map<String, Object> objectErrors, String message, Throwable cause) {
        super(ObjectErrorCode.INTERNAL_OBJECT_CLIENT, message, objectErrors, cause);
    }

    public InternalObjectClientException(Map<String, Object> objectErrors) {
        super(ObjectErrorCode.PAYLOAD_INVALID, DEFAULT_MESSAGE, objectErrors);
    }
}
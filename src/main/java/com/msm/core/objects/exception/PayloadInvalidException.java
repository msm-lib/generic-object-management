package com.msm.core.objects.exception;

import com.msm.core.exceptions.GenericBaseException;

import java.util.Map;

public class PayloadInvalidException extends GenericBaseException {
    public static final String FIELD_PARAM = "payload";
    private static final String DEFAULT_MESSAGE = "Payload invalid";

    public PayloadInvalidException(String fieldName, Throwable cause) {
        super(ObjectErrorCode.PAYLOAD_INVALID, DEFAULT_MESSAGE, Map.of(FIELD_PARAM, fieldName), cause);
    }

    public PayloadInvalidException(String fieldName, String message, Throwable cause) {
        super(ObjectErrorCode.PAYLOAD_INVALID, message, Map.of(FIELD_PARAM, fieldName), cause);
    }

    public PayloadInvalidException(String fieldName) {
        super(ObjectErrorCode.PAYLOAD_INVALID, DEFAULT_MESSAGE, Map.of(FIELD_PARAM, fieldName));
    }

    public PayloadInvalidException(String fieldName, String msg) {
        super(ObjectErrorCode.PAYLOAD_INVALID, msg, Map.of(FIELD_PARAM, fieldName));
    }
}

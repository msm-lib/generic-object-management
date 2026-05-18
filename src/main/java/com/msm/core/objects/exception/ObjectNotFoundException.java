package com.msm.core.objects.exception;

import com.msm.core.exceptions.GenericBaseException;

import java.util.Map;

public class ObjectNotFoundException extends GenericBaseException {
    public static final String FIELD_PARAM = "resource";
    private static final String DEFAULT_MESSAGE = "Object not found";

    public ObjectNotFoundException(String fieldName, Throwable cause) {
        super(ObjectErrorCode.OBJECT_NOT_FOUND, DEFAULT_MESSAGE, Map.of(FIELD_PARAM, fieldName), cause);
    }

    public ObjectNotFoundException(String fieldName, String message, Throwable cause) {
        super(ObjectErrorCode.OBJECT_NOT_FOUND, message, Map.of(FIELD_PARAM, fieldName), cause);
    }

    public ObjectNotFoundException(String fieldName) {
        super(ObjectErrorCode.OBJECT_NOT_FOUND, DEFAULT_MESSAGE, Map.of(FIELD_PARAM, fieldName));
    }

    public ObjectNotFoundException(String fieldName, String msg) {
        super(ObjectErrorCode.OBJECT_NOT_FOUND, msg, Map.of(FIELD_PARAM, fieldName));
    }
}

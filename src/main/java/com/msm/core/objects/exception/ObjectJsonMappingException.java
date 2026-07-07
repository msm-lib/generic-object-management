package com.msm.core.objects.exception;

public class ObjectJsonMappingException extends RuntimeException {
    public ObjectJsonMappingException(Throwable cause) {
        this.initCause(cause);
    }
}

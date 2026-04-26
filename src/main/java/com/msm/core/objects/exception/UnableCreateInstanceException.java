package com.msm.core.objects.exception;

public class UnableCreateInstanceException extends RuntimeException {
    public UnableCreateInstanceException(String message) {
        super(message);
    }
    public UnableCreateInstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}

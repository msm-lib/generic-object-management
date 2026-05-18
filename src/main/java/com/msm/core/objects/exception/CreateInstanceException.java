package com.msm.core.objects.exception;

public class CreateInstanceException extends RuntimeException {
    public CreateInstanceException(String message) {
        super(message);
    }
    public CreateInstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}

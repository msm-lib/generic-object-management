package com.msm.core.objects.exception;


import com.msm.core.exceptions.common.ErrorCode;

public enum ObjectErrorCode implements ErrorCode {
    OBJECT_NOT_FOUND("OBJECT_NOT_FOUND"),
    PAYLOAD_INVALID("PAYLOAD_INVALID");

    private final String code;

    ObjectErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}

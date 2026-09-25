package com.msm.core.objects.dataexchange.imports.model;

import com.msm.core.security.IdentifiableCode;

public enum FileProcessStatus implements IdentifiableCode {
    PROCESSING("processing"),
    FAILED("failed"),
    SUCCESS("success");

    private final String code;

    FileProcessStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
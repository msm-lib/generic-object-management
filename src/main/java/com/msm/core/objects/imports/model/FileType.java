package com.msm.core.objects.imports.model;

import com.msm.core.security.IdentifiableCode;

public enum FileType implements IdentifiableCode {
    EXCEL("excel"),
    CSV("csv");

    private final String code;

    FileType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}

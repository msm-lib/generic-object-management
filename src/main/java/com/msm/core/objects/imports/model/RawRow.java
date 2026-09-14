package com.msm.core.objects.imports.model;

public record RawRow<T>(long rowNumber, String objectName, T data) {
    public static <T> RawRow<T> of(long rowNumber, String objectName, T data) {
        return new RawRow<>(rowNumber, objectName, data);
    }
}

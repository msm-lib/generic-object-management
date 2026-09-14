package com.msm.core.objects.imports.model;

import java.util.UUID;


public record ColumnHeaderMapperContext<T>(
        UUID importId,
        long rowNumber,
        String objectName,
        T rowData
) {
    public static <T> ColumnHeaderMapperContext<T> of(UUID importId, long rowNumber, String objectName, T rowData) {
        return new ColumnHeaderMapperContext<>(importId, rowNumber, objectName, rowData);
    }
}

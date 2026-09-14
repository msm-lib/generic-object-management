package com.msm.core.objects.imports.model;

import java.util.Map;
import java.util.UUID;


public record RowMapperContext<T>(
        UUID importId,
        long rowNumber,
        String objectName,
        Map<Integer, String> headerColumn,
        T rowData
) {
    public static <T> RowMapperContext<T> of(UUID importId, long rowNumber, String objectName, Map<Integer, String> headerColumn, T rowData) {
        return new RowMapperContext<>(importId, rowNumber, objectName, headerColumn, rowData);
    }

    public static <T> RowMapperContext<T> of(UUID importId, long rowNumber, String objectName, T rowData) {
        return new RowMapperContext<>(importId, rowNumber, objectName, Map.of(), rowData);
    }
}

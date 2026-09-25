package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;
import java.util.UUID;


public record RowMappingContext<T>(
        UUID jobId,
        long rowNumber,
        String objectName,
        Map<Integer, String> headerColumn,
        T rowData
) {
    public static <T> RowMappingContext<T> of(UUID jobId, long rowNumber, String objectName, Map<Integer, String> headerColumn, T rowData) {
        return new RowMappingContext<>(jobId, rowNumber, objectName, headerColumn, rowData);
    }

    public static <T> RowMappingContext<T> of(UUID jobId, long rowNumber, String objectName, T rowData) {
        return new RowMappingContext<>(jobId, rowNumber, objectName, Map.of(), rowData);
    }
}

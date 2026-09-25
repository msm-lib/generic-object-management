package com.msm.core.objects.dataexchange.exports.model;

import java.util.Map;
import java.util.UUID;


public record ExportRowMappingContext<T>(
        UUID jobId,
        long rowNumber,
        String objectName,
        Map<Integer, ColumnHeaderPath> headerColumn,
        T rowData
) {
    public static <T> ExportRowMappingContext<T> of(UUID jobId, long rowNumber, String objectName, Map<Integer, ColumnHeaderPath> headerColumn, T rowData) {
        return new ExportRowMappingContext<>(jobId, rowNumber, objectName, headerColumn, rowData);
    }

    public static <T> ExportRowMappingContext<T> of(UUID jobId, long rowNumber, String objectName, T rowData) {
        return new ExportRowMappingContext<>(jobId, rowNumber, objectName, Map.of(), rowData);
    }
}

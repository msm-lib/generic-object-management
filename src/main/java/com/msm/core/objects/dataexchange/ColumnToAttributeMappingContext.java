package com.msm.core.objects.dataexchange;

import java.util.UUID;


public record ColumnToAttributeMappingContext<T>(
        UUID importId,
        long rowNumber,
        String objectName,
        T rowData
) {
    public static <T> ColumnToAttributeMappingContext<T> of(UUID importId, long rowNumber, String objectName, T rowData) {
        return new ColumnToAttributeMappingContext<>(importId, rowNumber, objectName, rowData);
    }
}

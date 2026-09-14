package com.msm.core.objects.imports.model;

import java.util.Map;

public record ImportRow(
        long rowNumber,
        Map<String, Object> rowData
) {
    public static ImportRow of(long rowNumber,  Map<String, Object> rowData) {
        return new ImportRow(rowNumber, rowData);
    }
}
package com.msm.core.objects.dataexchange.imports.model;

import java.util.List;
import java.util.UUID;

public record BatchRowData(
        UUID importId,
        String importObjectName,
        List<ImportRow> rowData
) {
    public static BatchRowData of(UUID importId, String importObjectName, List<ImportRow> dataRecords) {
        return new  BatchRowData(importId, importObjectName, dataRecords);
    }
}

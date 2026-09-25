package com.msm.core.objects.dataexchange.imports.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BatchInsertDataContext(
        UUID jobId,
        String objectName,
        List<Map<String, Object>> data
) {
    public static BatchInsertDataContext of(UUID jobId, String importObjectName, List<Map<String, Object>> data) {
        return new BatchInsertDataContext(jobId, importObjectName, data);
    }
}

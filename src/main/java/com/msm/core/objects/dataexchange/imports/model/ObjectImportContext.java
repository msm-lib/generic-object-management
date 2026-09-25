package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;
import java.util.UUID;

public record ObjectImportContext(
        String importObjectName,
        UUID jobId,
        Map<String, Object> param
) {
    public static ObjectImportContext of(String importObjectName, UUID jobId, Map<String, Object> param) {
        return new ObjectImportContext(importObjectName, jobId, param);
    }
}

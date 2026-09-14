package com.msm.core.objects.imports.model;

import java.util.Map;
import java.util.UUID;

public record ObjectImportContext(
        String importObjectName,
        UUID importJob,
        Map<String, Object> param
) {
    public static ObjectImportContext of(String importObjectName, UUID importJob, Map<String, Object> param) {
        return new ObjectImportContext(importObjectName, importJob, param);
    }
}

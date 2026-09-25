package com.msm.core.objects.dataexchange.imports.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReferenceResolveContext(
        UUID importId,
        String importObjectName,
        List<Map<String, Object>> data
) {
    public static ReferenceResolveContext of(UUID importId, String importObjectName, List<Map<String, Object>> data) {
        return new ReferenceResolveContext(importId, importObjectName, data);
    }
}

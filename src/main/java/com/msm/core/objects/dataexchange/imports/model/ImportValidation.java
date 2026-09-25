package com.msm.core.objects.dataexchange.imports.model;

import java.util.UUID;


public record ImportValidation(
        UUID importId,
        String importObjectName,
        String fileUrl
) {
    public static ImportValidation of(UUID importId, String importObjectName, String fileUrl) {
        return new ImportValidation(importId, importObjectName, fileUrl);
    }
}

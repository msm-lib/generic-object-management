package com.msm.core.objects.imports.model;

import java.util.UUID;

public record DownloadErrorContext (
        String objectName,
        UUID importId
){
    public static DownloadErrorContext of(String objectName, UUID importId) {
        return new DownloadErrorContext(objectName, importId);
    }
}

package com.msm.core.objects.dataexchange.imports.model;

import java.util.UUID;

public record DownloadErrorContext (
        String objectName,
        UUID importId
){
    public static DownloadErrorContext of(String objectName, UUID importId) {
        return new DownloadErrorContext(objectName, importId);
    }
}

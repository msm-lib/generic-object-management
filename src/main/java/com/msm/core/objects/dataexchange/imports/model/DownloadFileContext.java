package com.msm.core.objects.dataexchange.imports.model;

import com.msm.core.objects.dataexchange.FileType;

import java.util.UUID;

public record DownloadFileContext(
        UUID jobId,
        String objectName,
        FileType fileType
){
    public static DownloadFileContext of(UUID jobId, String objectName, FileType fileType) {
        return new DownloadFileContext(jobId, objectName, fileType);
    }
}

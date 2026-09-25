package com.msm.core.objects.dataexchange.imports.model;

import com.msm.core.objects.dataexchange.FileType;

import java.util.UUID;

public record FileProcessedEventContext(
        String objectName,
        UUID importId,
        FileType fileType,
        FileProcessStatus status
) {
    public static FileProcessedEventContext of(String objectName, UUID importId, FileType fileType, FileProcessStatus status) {
        return new FileProcessedEventContext(objectName, importId, fileType, status);
    }
}

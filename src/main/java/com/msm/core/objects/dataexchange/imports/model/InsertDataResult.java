package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;
import java.util.UUID;

public record InsertDataResult(
        UUID jobId,
        long rowNumber,
        ImportStatus status,
        String message,
        Map<String, Object> data
) {
}

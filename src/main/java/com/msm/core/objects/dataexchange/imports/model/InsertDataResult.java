package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;

public record InsertDataResult(
        long rowNumber,
        ImportStatus status,
        String message,
        Map<String, Object> data
) {
}

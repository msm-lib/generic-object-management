package com.msm.core.objects.imports.model;

import java.util.Map;

public record ImportErrorData(
        long rowNumber,
        String errorType,
        String fieldName,
        String errorCode,
        String message,
        Map<String, Object> data
) {
}

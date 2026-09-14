package com.msm.core.objects.imports.model;

import java.util.Map;

public record ImportRowResult(
        long rowNumber,
        ImportStatus status,
        String message,
        Map<String, Object> data
) {
}

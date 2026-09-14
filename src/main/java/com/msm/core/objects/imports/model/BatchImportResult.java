package com.msm.core.objects.imports.model;

import java.util.List;

public record BatchImportResult(
        long successCount,
        long failedCount,
        List<ImportRowResult> results
) {
}

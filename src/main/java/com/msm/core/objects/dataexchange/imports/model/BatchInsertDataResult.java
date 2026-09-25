package com.msm.core.objects.dataexchange.imports.model;

import java.util.List;

public record BatchInsertDataResult(
        long successCount,
        long failedCount,
        List<InsertDataResult> results
) {
}

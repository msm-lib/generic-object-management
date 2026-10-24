package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;
import java.util.UUID;

/** Payload of {@code import_parent_completed}: the details a job wrote into {@code parent}. */
public record ImportParentResult(UUID jobId, String objectName, ImportContext context, Map<String, Object> parent,
                                 long successRows, long errorRows) {
    public static ImportParentResult of(UUID jobId, String objectName, ImportContext context, Map<String, Object> parent,
                                        long successRows, long errorRows) {
        return new ImportParentResult(jobId, objectName, context, parent, successRows, errorRows);
    }
}

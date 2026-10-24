package com.msm.core.objects.dataexchange.imports.model;

import java.util.Map;
import java.util.UUID;

/** Payload of {@code import_parent_check}: the job about to import details into {@code parent}. */
public record ImportParentContext(UUID jobId, String objectName, ImportContext context, Map<String, Object> parent) {
    public static ImportParentContext of(UUID jobId, String objectName, ImportContext context, Map<String, Object> parent) {
        return new ImportParentContext(jobId, objectName, context, parent);
    }
}

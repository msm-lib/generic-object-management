package com.msm.core.objects.audit;

import com.msm.core.metadata.ObjectMetadata;

import java.util.Map;

public interface AuditStrategy {
    String DEFAULT_OBJECT_TYPE = "default";
    String supportObjectType();

    void apply(AuditAction action, ObjectMetadata meta, Map<String, Object> fields);
}

package com.msm.core.objects.audit;

import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.strategy.TypedStrategy;

import java.util.Map;

public interface AuditStrategy extends TypedStrategy<String> {
    String DEFAULT_OBJECT_TYPE = "default";
    void apply(AuditAction action, ObjectMetadata meta, Map<String, Object> fields);
}

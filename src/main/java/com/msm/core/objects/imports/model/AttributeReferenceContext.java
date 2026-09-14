package com.msm.core.objects.imports.model;

import com.msm.core.metadata.Attribute;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttributeReferenceContext (
        UUID importId,
        String importObjectName,
        Attribute attribute,
        List<Map<String, Object>> data
) {

    public static AttributeReferenceContext of(UUID importId, String importObjectName, Attribute attribute, List<Map<String, Object>> data){
        return new AttributeReferenceContext(importId, importObjectName, attribute, data);
    }
}

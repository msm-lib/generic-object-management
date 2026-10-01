package com.msm.core.objects.dataexchange.imports.model;

import com.msm.core.metadata.Attribute;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AttributeReferenceContext (
        UUID importId,
        String importObjectName,
        Attribute attribute,
        List<ImportRow> data,
        Set<AttributeReferenceFailed> attributeFailedRef
) {

    public static AttributeReferenceContext of(UUID importId, String importObjectName, Attribute attribute, List<ImportRow> data, Set<AttributeReferenceFailed> attributeFailedRef) {
        return new AttributeReferenceContext(importId, importObjectName, attribute, data, attributeFailedRef);
    }
}

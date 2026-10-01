package com.msm.core.objects.dataexchange.imports.model;

public record AttributeReferenceFailed(
        long rowNumber,
        String attributeName
) {
    public static AttributeReferenceFailed of(long rowNumber, String attributeName) {
        return new AttributeReferenceFailed(rowNumber, attributeName);
    }
}

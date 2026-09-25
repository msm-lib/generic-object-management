package com.msm.core.objects.dataexchange.exports.model;

public record ColumnHeaderPath(
        boolean isReference,
        String referenceFieldName,
        String originalPath
) {
    public static ColumnHeaderPath of(
        boolean isReference,
        String referenceFieldName,
        String originalPath) {
        return new ColumnHeaderPath(isReference, referenceFieldName, originalPath);
    }
}

package com.msm.core.objects.imports.model;

public record ColumnHeaderDefinitionPath(
        boolean isReference,
        String referenceFieldName,
        String originalPath
) {
    public static ColumnHeaderDefinitionPath of(
        boolean isReference,
        String referenceFieldName,
        String originalPath) {
        return new ColumnHeaderDefinitionPath(isReference, referenceFieldName, originalPath);
    }
}

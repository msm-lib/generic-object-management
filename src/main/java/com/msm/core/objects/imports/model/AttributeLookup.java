package com.msm.core.objects.imports.model;

import java.util.Set;

public record AttributeLookup(
        String attributeName,
        Set<String> defaultValues,
        boolean isKey
) {
    public static AttributeLookup ofDefault(String attributeName) {
        return of(attributeName, null, true);
    }

    public static AttributeLookup of(String attributeName, Set<String> defaultValues) {
        return of(attributeName, defaultValues, false);
    }

    public static AttributeLookup of(String attributeName, Set<String> defaultValues, boolean isKey) {
        return new AttributeLookup(attributeName, defaultValues, isKey);
    }
}

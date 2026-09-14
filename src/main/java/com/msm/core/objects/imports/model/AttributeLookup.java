package com.msm.core.objects.imports.model;

import java.util.Set;

public record AttributeLookup(
        String attributeName,
        Set<String> defaultValues
) {
    public static AttributeLookup of(String attributeName, Set<String> defaultValues) {
        return new AttributeLookup(attributeName, defaultValues);
    }
}

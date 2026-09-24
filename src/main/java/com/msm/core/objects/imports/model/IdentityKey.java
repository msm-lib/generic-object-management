package com.msm.core.objects.imports.model;

public record IdentityKey(
        String key,
        int level
) {
    public static IdentityKey of(
        String key,
        int level
    ) {
        return new IdentityKey(key, level);
    }
}

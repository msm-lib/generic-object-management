package com.msm.core.objects.integration.factory;

import com.msm.core.objects.integration.auth.common.AuthProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthProviderRegistry {
    private final Map<String, AuthProvider> providers = new ConcurrentHashMap<>();

    public void register(String name, AuthProvider provider) {
        providers.put(name, provider);
    }

    public AuthProvider get(String name) {
        return providers.get(name);
    }
}

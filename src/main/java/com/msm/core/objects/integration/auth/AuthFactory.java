package com.msm.core.objects.integration.auth;

import com.msm.core.objects.integration.auth.common.AuthProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthFactory {
//    private final List<AuthProvider> authProviders;
    private final Map<String, AuthProvider> CACHES = new ConcurrentHashMap<>();
    public AuthFactory(List<AuthProvider> authProviders) {
        authProviders.forEach(authProvider -> {});
    }


}

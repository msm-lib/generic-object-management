package com.msm.core.objects.integration.factory;

import com.msm.core.objects.integration.auth.common.AuthProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthProviderFactory {
    private final Map<String, AuthProvider> providers;

    public AuthProviderFactory(List<AuthProvider> authProviders) {

        providers = authProviders
                .stream()
                .collect(Collectors.toMap(AuthProvider::providerName, Function.identity()));
    }

    public AuthProvider get(String providerName) {

        AuthProvider provider = providers.get(providerName);

        if (provider == null) {
            throw new UnsupportedOperationException(providerName);
        }

        return provider;
    }

}

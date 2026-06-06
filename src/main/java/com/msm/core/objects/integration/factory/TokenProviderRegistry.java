package com.msm.core.objects.integration.factory;

import com.msm.core.objects.integration.auth.common.TokenProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TokenProviderRegistry {

    private final Map<String, TokenProvider> providers;

    public TokenProviderRegistry(List<TokenProvider> providers) {

        this.providers = providers
                .stream()
                .collect(Collectors.toMap(TokenProvider::supportProvider, Function.identity()));
    }

    public TokenProvider get(String provider) {
        return providers.get(provider);
    }
}

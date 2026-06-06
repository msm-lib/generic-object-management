package com.msm.core.objects.integration.auth.oauth2;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.factory.TokenProviderFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuth2AuthProvider implements AuthProvider {
    private final TokenProviderFactory tokenProviderFactory;

    @Override
    public String providerName() {
        return "oauth2";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        ctx.getHeaders().setBearerAuth(tokenProviderFactory.get(providerName()).getToken(ctx));
    }
}

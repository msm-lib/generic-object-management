package com.msm.core.objects.integration.auth.oauth2.password;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.factory.TokenProviderRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuth2PasswordAuthProvider implements AuthProvider {

    private final TokenProviderRegistry tokenProviderRegistry;

    @Override
    public String providerName() {
        return "oauth2-password";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        ctx.getHeaders().setBearerAuth(tokenProviderRegistry.get(providerName()).getToken(ctx));
    }
}

package com.msm.core.objects.integration.auth.bearer;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BearerAuthProvider implements AuthProvider {

    private final TokenProvider tokenProvider;

    @Override
    public String providerName() {
        return "bearer-token";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        ctx.getHeaders().setBearerAuth(tokenProvider.getToken(ctx));
    }
}
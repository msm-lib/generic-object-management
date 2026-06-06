package com.msm.core.objects.integration.auth.basic;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class BasicUsernamePasswordProvider implements AuthProvider {

//    private final BasicCredentials credentials;

    @Override
    public String providerName() {
        return "basic-username-password";
    }

    @Override
    public void apply(HttpRequestContext ctx) {

        ctx.getHeaders().setBasicAuth(
                String.valueOf(ctx.getAuthConfig().getProperties().get("username")),
                String.valueOf(ctx.getAuthConfig().getProperties().get("password")));
    }
}

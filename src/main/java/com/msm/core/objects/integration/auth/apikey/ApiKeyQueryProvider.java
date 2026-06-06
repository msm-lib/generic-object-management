package com.msm.core.objects.integration.auth.apikey;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiKeyQueryProvider implements AuthProvider {

    @Override
    public String providerName() {
        return "apikey-query";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        ctx.getQueryParams().put(
                String.valueOf(ctx.getAuthConfig().getProperties().get("name")),
                String.valueOf(ctx.getAuthConfig().getProperties().get("value")));
    }
}
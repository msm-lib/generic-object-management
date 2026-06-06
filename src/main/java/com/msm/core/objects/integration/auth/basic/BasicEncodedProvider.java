package com.msm.core.objects.integration.auth.basic;

import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;

public class BasicEncodedProvider implements AuthProvider {

    @Override
    public String providerName() {
        return "basic-encoded";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        ctx.getHeaders().add("Authorization", "Basic " + ctx.getAuthConfig().getProperties().get("credential"));
    }
}
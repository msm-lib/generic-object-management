package com.msm.core.objects.integration.auth.bearer;

import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;

public class StaticBearerAuthProvider implements AuthProvider {

    @Override
    public String providerName() {
        return "static-bearer-token";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        Object token = ctx.getAuthConfig().getProperties().get("token");
        if(token == null){
            throw ObjectErrors.notFound("token");
        }
        ctx.getHeaders().setBearerAuth(String.valueOf(token));
    }
}
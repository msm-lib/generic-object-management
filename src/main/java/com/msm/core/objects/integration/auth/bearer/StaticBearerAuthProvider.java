package com.msm.core.objects.integration.auth.bearer;

import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;

import java.util.Objects;

public class StaticBearerAuthProvider implements AuthProvider {

    @Override
    public String providerName() {
        return "static-bearer-token";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        Object token = ctx.getAuthConfig().getProperties().get(ObjectConstants.TOKEN_NAME);
        if(token == null){
            throw ObjectErrors.notFound(ObjectConstants.TOKEN_NAME);
        }

        Object tokenHeaderName = ctx.getAuthConfig().getProperties().get(ObjectConstants.TOKEN_HEADER_NAME);
        if(Objects.isNull(tokenHeaderName)) {
            ctx.getHeaders().setBearerAuth(String.valueOf(token));
        } else {
            ctx.getHeaders().add(tokenHeaderName.toString(), token.toString());
        }
    }
}
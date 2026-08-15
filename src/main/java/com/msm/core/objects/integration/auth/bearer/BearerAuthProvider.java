package com.msm.core.objects.integration.auth.bearer;

import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class BearerAuthProvider implements AuthProvider {

    private final TokenProvider tokenProvider;

    @Override
    public String providerName() {
        return "bearer-token";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        String token = tokenProvider.getToken(ctx);

        Object tokenHeaderName = ctx.getAuthConfig().getProperties().get(ObjectConstants.TOKEN_HEADER_NAME);
        if(Objects.isNull(tokenHeaderName)) {
            ctx.getHeaders().setBearerAuth(token);
        } else {
            ctx.getHeaders().add(tokenHeaderName.toString(), token);
        }
    }
}
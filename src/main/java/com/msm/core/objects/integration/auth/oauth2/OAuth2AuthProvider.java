package com.msm.core.objects.integration.auth.oauth2;

import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.factory.TokenProviderFactory;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class OAuth2AuthProvider implements AuthProvider {
    private final TokenProviderFactory tokenProviderFactory;

    @Override
    public String providerName() {
        return "oauth2-credentials";
    }

    @Override
    public void apply(HttpRequestContext ctx) {
        String token = tokenProviderFactory.get(providerName()).getToken(ctx);

        Object tokenHeaderName = ctx.getAuthConfig().getProperties().get(ObjectConstants.TOKEN_HEADER_NAME);
        if(Objects.isNull(tokenHeaderName)) {
            ctx.getHeaders().setBearerAuth(token);
        } else {
            ctx.getHeaders().add(tokenHeaderName.toString(), token);
        }
    }
}

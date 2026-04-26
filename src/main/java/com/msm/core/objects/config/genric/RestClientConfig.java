package com.msm.core.objects.config.genric;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class RestClientConfig {
    private static final String X_TENANT = "x-tenant";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer";

    @Bean(name = "requestInitializer")
    public ClientHttpRequestInitializer requestInitializer() {
        return request -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication != null && authentication.isAuthenticated() && (authentication.getPrincipal() instanceof UserDetailsImpl)) {
//                String tenantCode = ((UserDetailsImpl) authentication.getPrincipal()).getTenantCode();
//                if(!request.getHeaders().containsKey(X_TENANT)) {
//                    request.getHeaders().add(X_TENANT, tenantCode);
//                }
//                String token = ((UserDetailsImpl) authentication.getPrincipal()).getTokenValue();
//                if(!request.getHeaders().containsKey(AUTHORIZATION_HEADER)) {
//                    request.getHeaders().add(AUTHORIZATION_HEADER, String.format("%s %s", TOKEN_TYPE, token));
//                }
//            }
        };
    }
}
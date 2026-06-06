package com.msm.core.objects.integration.middleware;

import com.msm.core.objects.entity.enums.IntegrationStatus;
import com.msm.core.objects.integration.context.ExecutionEvent;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.exception.AuthenticationException;
import com.msm.core.objects.integration.factory.AuthProviderFactory;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class AuthMiddleware extends AbstractMiddleware {
    private final AuthProviderFactory authProviderFactory;

    @Override
    public int order() {
        return 100;
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public void beforeExecute(HttpRequestContext context) {
        try {
            String provider = context.getAuthProvider();
            if (provider == null) return;
            if ("mtls".equals(provider)) {
                return;
            }
            authProviderFactory.get(context.getAuthConfig().getProvider()).apply(context);
            context.addEvent(ExecutionEvent.builder()
                    .timestamp(Instant.now())
                    .component(name())
                    .componentExecution(name())
                    .action("authenticate")
                    .status(IntegrationStatus.SUCCESS.name())
                    .build()
            );
        } catch (Exception ex) {
            context.addEvent(ExecutionEvent.builder()
                    .timestamp(Instant.now())
                    .component(name())
                    .componentExecution(name())
                    .action("authenticate")
                    .status(IntegrationStatus.FAILED.name())
                    .message(ex.getMessage())
                    .errorType(ex.getClass().getSimpleName())
                    .build()
            );

            throw new AuthenticationException();
        }
    }
}
package com.msm.core.objects.integration.retry;

import com.msm.core.objects.entity.enums.IntegrationStatus;
import com.msm.core.objects.integration.context.ExecutionEvent;
import com.msm.core.objects.integration.context.HttpRequestContext;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class ResilienceRetryExecutor implements RetryExecutor {

    private final RetryConfigResolver resolver;

    @Override
    public <T> T execute(HttpRequestContext context, Supplier<T> supplier) {

        Retry retry = resolver.resolve(context);
        registerEvents(context, retry);
        return Retry.decorateSupplier(
                retry,
                supplier
        ).get();
    }

    private void registerEvents(HttpRequestContext context, Retry retry) {

        retry.getEventPublisher().onRetry(event -> {

            context.addEvent(ExecutionEvent.builder()
                            .timestamp(Instant.now())
                            .component("RetryMiddleware")
                            .componentExecution(context.getComponentExecution())
                            .action("retry")
                            .status("RETRYING")
                            .attempt(event.getNumberOfRetryAttempts())
                            .message(event.getLastThrowable() != null
                                    ? event.getLastThrowable().getMessage()
                                    : null
                            )
                            .build()
                    );

                })
                .onSuccess(event -> {
                    context.addEvent(ExecutionEvent.builder()
                            .timestamp(Instant.now())
                            .component("RetryMiddleware")
                            .componentExecution(context.getComponentExecution())
                            .action("retry")
                            .status(IntegrationStatus.SUCCESS.name())
                            .attempt(event.getNumberOfRetryAttempts())
                            .build()
                    );
                })
                .onError(event -> {
                    context.addEvent(ExecutionEvent.builder()
                            .timestamp(Instant.now())
                            .component("RetryMiddleware")
                            .componentExecution(context.getComponentExecution())
                            .action("retry")
                            .status(IntegrationStatus.FAILED.name())
                            .attempt(event.getNumberOfRetryAttempts())
                            .message(event.getLastThrowable() != null
                                    ? event.getLastThrowable().getMessage()
                                    : null
                            )
                            .build()
                    );
                });
    }

}

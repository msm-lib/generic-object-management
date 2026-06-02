package com.msm.core.objects.integration;

import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.integration.middleware.HttpMiddlewareChain;
import com.msm.core.objects.integration.retry.RetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;

@RequiredArgsConstructor
@Slf4j
public class IntegrationClientExchange {
    private final RequestClient requestClient;
    private final HttpMiddlewareChain chain;
    private final RetryExecutor retryExecutor;

    public <T> T exchange(HttpRequestContext context, Class<T> responseType) {

        try {
            chain.before(context);
            T response = handleRequest(context, responseType);
            chain.after(context, response);
            return response;
        } catch (Exception ex) {
            chain.error(context, ex);
            throw ex;
        }
    }

    public <T> T exchange(HttpRequestContext context, ParameterizedTypeReference<T> responseType) {

        try {

            chain.before(context);

            T response = handleRequest(context, responseType);

            chain.after(context, response);

            return response;

        } catch (Exception ex) {
            chain.error(context, ex);
            throw ex;
        }
    }

    private <T> T handleRequest(HttpRequestContext context, Class<T> responseType) {
        context.setComponentExecution(getClass().getSimpleName()+ ".handleRequest");
        return retryExecutor.execute(context, () -> execute(context, responseType));
    }

    private <T> T handleRequest(HttpRequestContext context, ParameterizedTypeReference<T> responseType) {
        context.setComponentExecution(getClass().getSimpleName()+ ".handleRequest");
        return retryExecutor.execute(context, () -> execute(context, responseType));
    }

    private <T> T execute(HttpRequestContext context, Class<T> responseType) {

        return requestClient.exchange(
                context.getBaseUrl(),
                context.getPath(),
                context.getMethod(),
                context.getHeaders(),
                context.getQueryParams(),
                context.getBody(),
                responseType
        );
    }

    private <T> T execute(HttpRequestContext context, ParameterizedTypeReference<T> responseType) {

        return requestClient.exchange(
                context.getBaseUrl(),
                context.getPath(),
                context.getMethod(),
                context.getHeaders(),
                context.getQueryParams(),
                context.getBody(),
                responseType
        );
    }
}

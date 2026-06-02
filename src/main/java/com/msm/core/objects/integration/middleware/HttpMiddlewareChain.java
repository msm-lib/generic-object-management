package com.msm.core.objects.integration.middleware;

import com.msm.core.objects.integration.context.HttpRequestContext;

import java.util.Comparator;
import java.util.List;

public class HttpMiddlewareChain {

    private final List<Middleware> middlewares;

    public HttpMiddlewareChain(List<Middleware> middlewares) {

        this.middlewares = middlewares
                .stream()
                .sorted(Comparator.comparingInt(Middleware::order))
                .toList();
    }

    public void before(HttpRequestContext context) {
        middlewares.forEach(m -> m.before(context));
    }

    public void after(HttpRequestContext context, Object response) {
        middlewares.forEach(m -> m.after(context, response));
    }

    public void error(HttpRequestContext context, Exception exception) {
        middlewares.forEach(m -> m.onError(context, exception));
    }
}

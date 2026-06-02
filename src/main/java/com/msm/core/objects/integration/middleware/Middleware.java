package com.msm.core.objects.integration.middleware;

import com.msm.core.objects.integration.context.HttpRequestContext;

public interface Middleware {
    int order();
    String name();
    void before(HttpRequestContext context);

    default void after(HttpRequestContext context, Object response) {}

    default void onError(HttpRequestContext context, Exception exception) {
    }
}
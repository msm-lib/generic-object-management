package com.msm.core.objects.integration.middleware;

import com.msm.core.objects.integration.context.HttpRequestContext;

public abstract class AbstractMiddleware implements Middleware {

    @Override
    public void before(HttpRequestContext context) {
        context.setComponentExecution(name() + ".before");
        beforeExecute(context);
    }

    @Override
    public void after(HttpRequestContext context, Object response) {
        context.setComponentExecution(name() + ".after");
        afterExecute(context, response);
    }

    @Override
    public void onError(HttpRequestContext context, Exception exception) {
        context.setComponentExecution(name() + ".error");
        onErrorExecute(context, exception);
    }

    abstract public void beforeExecute(HttpRequestContext context);
    public void afterExecute(HttpRequestContext context, Object response) {}
    public void onErrorExecute(HttpRequestContext context, Exception exception){}
}

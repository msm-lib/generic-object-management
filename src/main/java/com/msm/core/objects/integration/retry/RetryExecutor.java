package com.msm.core.objects.integration.retry;

import com.msm.core.objects.integration.context.HttpRequestContext;

import java.util.function.Supplier;

public interface RetryExecutor {
    <T> T execute(HttpRequestContext context, Supplier<T> supplier);
}

package com.msm.core.objects.integration.auth.common;

import com.msm.core.objects.integration.context.HttpRequestContext;

public interface TokenProvider {
    String supportProvider();
    String getToken(HttpRequestContext ctx);
}

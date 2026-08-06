package com.msm.core.objects.integration.data.outh2;

import lombok.Data;

import java.util.Map;

@Data
public class OAuth2Properties {

    private String tokenUrl;

    private String clientId;

    private String clientSecret;

    private String grantType = "client_credentials";

    private String scope;

    private String accessTokenPath;

    private Map<String, String> headers;

    // optional
    private int maxAttempts = 3;
    private long waitDurationMs = 200;
    private long connectTimeoutMs = 3000;

    private long readTimeoutMs = 5000;

    private boolean cacheToken = true;

    private long skewSeconds = 60;
    private boolean strictJsonMode = false;
}
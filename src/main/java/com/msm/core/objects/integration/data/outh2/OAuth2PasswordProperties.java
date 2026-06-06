package com.msm.core.objects.integration.data.outh2;

import lombok.Data;

@Data
public class OAuth2PasswordProperties {

    private String tokenUrl;

    private String username;
    private String password;

    private String accessTokenPath;

    // optional
    private int maxAttempts = 3;
    private long waitDurationMs = 200;
    private long connectTimeoutMs = 3000;

    private long readTimeoutMs = 5000;

    private boolean cacheToken = true;

    private long skewSeconds = 60;
    private boolean strictJsonMode = false;
}
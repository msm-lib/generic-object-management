package com.msm.core.objects.integration.data.outh2;

import com.msm.core.objects.integration.auth.enums.OAuth2GrantType;
import com.msm.core.objects.integration.data.BasicCredentials;
import lombok.Data;

@Data
public class OAuth2Properties {

    private String tokenUrl;

    private String clientId;

    private String clientSecret;

    private OAuth2GrantType grantType = OAuth2GrantType.CLIENT_CREDENTIALS;

    private String scope;

    private BasicCredentials credential;

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
package com.msm.core.objects.integration.data;

import com.msm.core.objects.integration.auth.apikey.ApiKeyProperties;
import com.msm.core.objects.integration.data.outh2.OAuth2Properties;
import lombok.Data;

@Data
public class AuthProviderProperties {
    private String type;

    // bearer
    private String token;

    // basic
    private BasicCredentials basic;

    // apikey
    private ApiKeyProperties apikey;

    // oauth2
    private OAuth2Properties oauth2;

    // mtls
    private MtlsProperties mtls;
}

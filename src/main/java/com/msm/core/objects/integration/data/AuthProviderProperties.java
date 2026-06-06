package com.msm.core.objects.integration.data;

import lombok.Data;

import java.util.Map;

@Data
public class AuthProviderProperties {
//    private String type;

    // bearer
//    private String token;

//    // basic
//    private BasicCredentials basic;
//
//    // apikey
//    private ApiKeyProperties apikey;
//
//    // oauth2
//    private OAuth2Properties oauth2;
//
//    // mtls
//    private MtlsProperties mtls;

//    private AuthConfig auth;
    private String provider;

    private Map<String, Object> properties;
}

package com.msm.core.objects.integration.data;

import com.msm.core.commons.Utils;
import lombok.Data;

import java.util.HashMap;
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

    public void setProperties(Map<String, String> rawProperties) {
        if (rawProperties == null) return;

        this.properties = new HashMap<>();
        rawProperties.forEach((key, value) -> {
            String camelKey = Utils.STR.toCamelCase(key, false, '-');
            this.properties.put(camelKey, value);
        });
    }
}

package com.msm.core.objects.integration.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProviderEnum implements Provider {
    API_KEY_HEADER("apikey-header"),
    API_KEY_QUERY("apikey-query"),
    BASIC_ENCODED("basic-encoded"),
    BASIC_PASSWORD("basic-password"),
    BEARER_TOKEN("bearer-token"),
    OAUTH2_CLIENT_CREDENTIALS("oauth2-credentials"),
    OAUTH2_PASSWORD("oauth2-password")


    ;


    private final String name;

}

package com.msm.core.objects.integration.auth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuth2GrantType {
    CLIENT_CREDENTIALS("client_credentials"),
    CLIENT_PASSWORD("client_password"),;

    private final String value;

}

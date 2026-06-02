package com.msm.core.objects.integration.data.outh2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OAuth2PasswordTokenResponse {

    @JsonProperty("token")
    private String accessToken;
}
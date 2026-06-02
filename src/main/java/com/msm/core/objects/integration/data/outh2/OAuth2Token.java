package com.msm.core.objects.integration.data.outh2;

import lombok.Data;

import java.time.Instant;

@Data
public class OAuth2Token {

    private String accessToken;

    private long expiresIn;

    private Instant createdAt;

    public boolean isExpired(long skewSeconds) {

        return Instant.now().isAfter(
                createdAt.plusSeconds(expiresIn - skewSeconds)
        );
    }
}
package com.msm.core.objects.integration.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static String cleanToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public static DecodedJWT decodeToken(String token) {
        String jwtToken = cleanToken(token);
        return JWT.decode(jwtToken);
    }

    public static String getSubject(String token) {
        return decodeToken(token).getSubject();
    }

    public static Date getExpirationDate(String token) {
        return decodeToken(token).getExpiresAt();
    }

    public static boolean isTokenExpired(String token) {
        Date expiration = getExpirationDate(token);
        return expiration != null && expiration.before(new Date());
    }

    public static long getRemainingTimeMs(String token) {
        Date expiration = getExpirationDate(token);
        if (expiration == null) return 0;

        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public static String getClaimString(String token, String claimName) {
        return decodeToken(token).getClaim(claimName).asString();
    }

    public static Map<String, Object> getAllClaims(String token) {
        return decodeToken(token).getClaims().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().as(Object.class)
                ));
    }
}

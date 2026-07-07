package com.msm.core.objects.integration.auth;

import java.util.regex.Pattern;

public final class RedisKeyGeneratorOptimized {
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[:/.]");
    private static final Pattern DUPLICATE_UNDERSCORES = Pattern.compile("_+");

    public static String generateUrlKey(String rawInputUrl) {
        String cleanUrl = SPECIAL_CHARS.matcher(rawInputUrl).replaceAll("_");
        cleanUrl = DUPLICATE_UNDERSCORES.matcher(cleanUrl).replaceAll("_");

        return cleanUrl;
    }
}

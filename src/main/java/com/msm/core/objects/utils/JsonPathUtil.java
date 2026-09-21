package com.msm.core.objects.utils;

import com.jayway.jsonpath.JsonPath;

public class JsonPathUtil {

    public static <X> X extractValue(Object data, String userPath) {
        if (data == null || userPath == null || userPath.trim().isEmpty()) {
            return null;
        }

        try {
            return JsonPath.read(data, normalizePath(userPath));
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String normalizePath(String userPath) {
        String targetPath = userPath.trim();
        if (!targetPath.startsWith("$.")) {
            if (targetPath.startsWith("$")) {
                targetPath = "$." + targetPath.substring(1);
            } else {
                targetPath = "$." + targetPath;
            }
        }
        return targetPath;
    }
}


package com.msm.core.objects.integration;

import com.jayway.jsonpath.JsonPath;

public class IntegrationJsonUtil {

    public static <X> X extractValue(Object responsePayload, String userPath) {
        if (responsePayload == null || userPath == null || userPath.trim().isEmpty()) {
            return null;
        }
        String targetPath = userPath.trim();
        if (!targetPath.startsWith("$.")) {
            if (targetPath.startsWith("$")) {
                targetPath = "$." + targetPath.substring(1);
            } else {
                targetPath = "$." + targetPath;
            }
        }

        try {
            return JsonPath.read(responsePayload, targetPath);
        } catch (Exception ignore) {
            return null;
        }
    }
}


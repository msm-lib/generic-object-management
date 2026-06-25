package com.msm.core.objects.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.commons.Utils;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class RequestUtils {
    private RequestUtils() {}

    public static <X> X getHeader(HttpServletRequest request, String key, Class<X> clazz) {
        String obj = request.getHeader(key);
        return Utils.O.convertObject(obj, clazz);
    }

    public static <X> X getHeader(HttpServletRequest request, String key, TypeReference<X> clazz) {
        String obj = request.getHeader(key);
        return Utils.O.convertObject(obj, clazz);
    }

    public static String getHeader(HttpServletRequest request, String key) {
        return request.getHeader(key);
    }

    public static <X> X getHeaders(HttpServletRequest request, String key, TypeReference<X> typeRef) {
        try {
            String obj = request.getHeader(key);
            if (Utils.STR.isBlank(obj)) {
                return null;
            }
            return Utils.O.read(obj, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Error while parse header JSON: " + key, e);
        }
    }

}

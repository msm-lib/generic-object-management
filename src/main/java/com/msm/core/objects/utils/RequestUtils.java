package com.msm.core.objects.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.commons.Utils;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

    public static <X> Set<X> getHeaders(HttpServletRequest request, String key) {
        try {
            String obj = request.getHeader(key);
            if(Utils.STR.isBlank(obj)) return new HashSet<>();
            return Utils.O.read(request.getHeader(key), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

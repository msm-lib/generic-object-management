package com.msm.core.objects.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.objects.utils.RequestUtils;
import com.msm.core.security.PermissionHelper;
import com.msm.core.security.context.DataScopeContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.Set;

@Slf4j
public class ObjectSecurityUtils {
    public static final String AUTH_TOKEN_HEADER = "Authorization";
    public static final String AUTH_TOKEN_PREFIX = "Bearer ";
    public static final String TENANT = "x-tenant";
    public static final String TEAMS = "x-team-ids";
    public static final String ORGS = "x-org-ids";
    public static final String PARENT_CHILDS = "x-child-ids";
    public static final String OWNER = "x-user-id";
    public static final Set<String> SECURITY_HEADERS = Set.of(
            TENANT,
            TEAMS,
            ORGS,
            PARENT_CHILDS,
            OWNER
    );

    public static DataScopeContext getDataScopeContext(HttpServletRequest request) {
        return PermissionHelper.createDataScopeContext(
                parseHeader(request, TEAMS, new TypeReference<>() {}),
                parseHeader(request, ORGS, new TypeReference<>() {}),
                parseHeader(request, PARENT_CHILDS, new TypeReference<>() {}),
                RequestUtils.getHeader(request, OWNER, new TypeReference<>() {})
        );
    }

    private static  <X> X parseHeader(HttpServletRequest request, String key, TypeReference<X> typeReference) {
        try {
            return RequestUtils.getHeaders(request, key, typeReference);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static void logSecurityHeaders(HttpServletRequest request) {
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            log.debug("--- [START] HTTP REQUEST HEADERS ---");
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (SECURITY_HEADERS.contains(headerName)) {
                    java.util.Enumeration<String> headerValues = request.getHeaders(headerName);
                    java.util.List<String> valuesList = java.util.Collections.list(headerValues);
                    log.debug("{}: {}", headerName, String.join(", ", valuesList));
                }
            }
            log.debug("--- [END] HTTP REQUEST HEADERS ---");
        }
    }

    public static void forwardHeader(HttpRequest request) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest currentRequest = attributes.getRequest();
            Enumeration<String> headerNames = currentRequest.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if(SECURITY_HEADERS.contains(headerName)){
                        String headerValue = currentRequest.getHeader(headerName);
                        request.getHeaders().set(headerName, headerValue);
                    }
                }
            }
        }
    }
}

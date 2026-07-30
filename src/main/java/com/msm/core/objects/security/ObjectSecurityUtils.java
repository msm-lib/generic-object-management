package com.msm.core.objects.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.commons.Utils;
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
import java.util.UUID;

@Slf4j
public class ObjectSecurityUtils {
    public static final String AUTH_TOKEN_HEADER = "Authorization";
    public static final String AUTH_TOKEN_PREFIX = "Bearer ";
    public static final String TENANT = "x-tenant";
    public static final String TEAMS = "x-team-ids";
    public static final String ORG_IDS = "x-org-ids";
    public static final String CHILD_IDS = "x-child-ids";
    public static final String PARENT_IDS = "x-parent-ids";
    public static final String OWNER = "x-user-id";
    public static final Set<String> SECURITY_HEADERS = Set.of(
            TENANT,
            TEAMS,
            ORG_IDS,
            CHILD_IDS,
            PARENT_IDS,
            OWNER
    );

    public static DataScopeContext getDataScopeContext(HttpServletRequest request) {
        Set<UUID> teamIdsHeader = Utils.CL.emptyIfNull(parseHeader(request, TEAMS, new TypeReference<Set<UUID>>() {}));
        Set<UUID> orgIdsHeader = Utils.CL.emptyIfNull(parseHeader(request, ORG_IDS, new TypeReference<Set<UUID>>() {}));
        Set<UUID> childIdsHeader = Utils.CL.emptyIfNull(parseHeader(request, CHILD_IDS, new TypeReference<Set<UUID>>() {}));
        Set<UUID> parentIdsHeader = Utils.CL.emptyIfNull(parseHeader(request, PARENT_IDS, new TypeReference<Set<UUID>>() {}));
        UUID ownerIdHeader = RequestUtils.getHeader(request, OWNER, new TypeReference<>() {});


        childIdsHeader.addAll(orgIdsHeader);//for parent child
        parentIdsHeader.addAll(childIdsHeader);//for parent child parent

        return PermissionHelper.createDataScopeContext(
                teamIdsHeader,
                orgIdsHeader,
                childIdsHeader,
                parentIdsHeader,
                ownerIdHeader
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

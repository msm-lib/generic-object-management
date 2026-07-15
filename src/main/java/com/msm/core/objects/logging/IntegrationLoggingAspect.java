package com.msm.core.objects.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msm.core.objects.entity.enums.IntegrationStatus;
import com.msm.core.security.context.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Aspect
@RequiredArgsConstructor
@Slf4j
public class IntegrationLoggingAspect {

    private final IntegrationLogWriter integrationLogWriter;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final List<IntegrationErrorResolver> errorResolvers;

    @Around("@annotation(integrationLogging)")
    public Object around(ProceedingJoinPoint joinPoint, IntegrationLogging integrationLogging) throws Throwable {
        RequestContext ctx = com.msm.core.security.RequestContextHolder.getRequestContext();
        Instant createdAt = Instant.now();
        IntegrationLogData.IntegrationLogDataBuilder builder = IntegrationLogData.builder()
                .connector(integrationLogging.connector())
                .operation(integrationLogging.operation())
                .createdAt(createdAt)
                .tenantId(ctx.getTenantCode())
                .traceId(LogTraceUtils.getTraceIdLog())
                .spanId(LogTraceUtils.getSpanIdLog())
                .environment(resolveEnvironment());

        populateRequestContext(builder);
        builder.requestBody(serialize(resolveRequestBody(joinPoint)));

        try {
            Object result = joinPoint.proceed();
            populateSuccess(builder, result);
            return result;
        } catch (Throwable ex) {
            populateFailure(builder, ex);
            throw ex;
        } finally {
            Instant completedAt = Instant.now();
            builder.completedAt(completedAt)
                    .durationMs(Duration.between(createdAt, completedAt).toMillis());
            writeSafely(builder.build());
        }
    }

    private void populateRequestContext(IntegrationLogData.IntegrationLogDataBuilder builder) {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            return;
        }
        builder.endpoint(request.getRequestURI())
                .method(request.getMethod())
                .requestParam(request.getQueryString())
                .correlationId(request.getHeader(LogTraceUtils.HEADER_CORRELATION_ID));
    }

    private void populateSuccess(IntegrationLogData.IntegrationLogDataBuilder builder, Object result) {
        int statusCode = HttpStatus.OK.value();
        Object body = result;
        if (result instanceof ResponseEntity<?> responseEntity) {
            statusCode = responseEntity.getStatusCode().value();
            body = responseEntity.getBody();
        }
        builder.status(IntegrationStatus.SUCCESS)
                .statusCode(statusCode)
                .responseBody(serialize(body));
    }

    private void populateFailure(IntegrationLogData.IntegrationLogDataBuilder builder, Throwable ex) {
        builder.status(IntegrationStatus.FAILED);
        for (IntegrationErrorResolver resolver : errorResolvers) {
            if (resolver.supports(ex)) {
                resolver.resolve(ex, builder);
                return;
            }
        }
        builder.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorMessage(ex.getMessage());
    }

    private Object resolveRequestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof RequestBody) {
                    return args[i];
                }
            }
        }
        return args.length > 0 ? args[0] : null;
    }

    private HttpServletRequest resolveRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String resolveEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        String[] effective = profiles.length > 0 ? profiles : environment.getDefaultProfiles();
        return String.join(",", effective);
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize integration log payload", e);
            return null;
        }
    }

    private void writeSafely(IntegrationLogData data) {
        try {
            integrationLogWriter.write(data);
        } catch (Exception e) {
            log.warn("Failed to enqueue integration log write", e);
        }
    }
}

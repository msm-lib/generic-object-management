package com.msm.core.objects.integration.middleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.enums.IntegrationStatus;
import com.msm.core.objects.entity.metadata.IntegrationLogMeta;
import com.msm.core.objects.exception.integration.IntegrationException;
import com.msm.core.objects.integration.context.HttpRequestContext;
import com.msm.core.objects.logging.LogTraceUtils;
import com.msm.core.objects.service.IntegrationLogService;
import com.msm.core.security.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

@RequiredArgsConstructor
@Slf4j
public class TracingMiddleware extends AbstractMiddleware {

    private final IntegrationLogService integrationLogService;

    @Override
    public int order() {
        return 300;
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public void beforeExecute(HttpRequestContext context) {

        log.info(
                "[HTTP] {}",
                context
        );

        DataRecord logRecord = DataRecord.of(new HashMap<>());
        logRecord.set(IntegrationLogMeta.CREATED_AT, Instant.now());
        logRecord.set(IntegrationLogMeta.CONNECTOR, context.getConnectorName());
        logRecord.set(IntegrationLogMeta.METHOD, context.getMethod().name());
        logRecord.set(IntegrationLogMeta.ENDPOINT, context.resolveUrl());
        logRecord.set(IntegrationLogMeta.TRACE_ID, LogTraceUtils.getTraceIdLog());
        logRecord.set(IntegrationLogMeta.SPAN_ID, LogTraceUtils.getSpanIdLog());
        logRecord.set(IntegrationLogMeta.TENANT_ID, RequestContextHolder.getRequestContext().getTenantCode());
        logRecord.set(IntegrationLogMeta.AUTH_PROVIDER, context.getAuthProvider());
        context.put("integrationLog", logRecord);
    }

    @Override
    public void afterExecute(HttpRequestContext context, Object response) {
        log.info(
                "[HTTP-SUCCESS] {} {}",
                context.getMethod(),
                context.resolveUrl()
        );

        DataRecord logRecord = context.get("integrationLog");
        logRecord.set(IntegrationLogMeta.COMPLETED_AT, Instant.now());
        logRecord.set(IntegrationLogMeta.DURATION_MS, Duration.between(
                logRecord.get(IntegrationLogMeta.CREATED_AT),
                logRecord.get(IntegrationLogMeta.COMPLETED_AT)
        ).toMillis());
        logRecord.set(IntegrationLogMeta.STATUS_CODE, HttpStatus.OK.value());
        logRecord.set(IntegrationLogMeta.STATUS, IntegrationStatus.SUCCESS.name());
        try {
            logRecord.set(IntegrationLogMeta.REQUEST_PARAM, Utils.O.toJsonString(context.getQueryParams()));
            logRecord.set(IntegrationLogMeta.REQUEST_BODY, Utils.O.toJsonString(context.getBody()));
            logRecord.set(IntegrationLogMeta.RESPONSE_BODY, Utils.O.toJsonString(response));
            logRecord.set(IntegrationLogMeta.STEP_ERROR_MESSAGE, JSONB.valueOf(Utils.O.toJsonString(context.getEvents())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        integrationLogService.createIntegrationLog(logRecord.getValues());
    }

    @Override
    public void onErrorExecute(HttpRequestContext context, Exception ex) {

        log.error(
                "[HTTP-ERROR] {} {}",
                context.getMethod(),
                context.resolveUrl(),
                ex
        );

        DataRecord logRecord = context.get("integrationLog");
        if(logRecord == null) {
            logRecord = DataRecord.of(new HashMap<>());
            logRecord.set(IntegrationLogMeta.CREATED_AT, Instant.now());
            logRecord.set(IntegrationLogMeta.CONNECTOR, context.getConnectorName());
            logRecord.set(IntegrationLogMeta.METHOD, context.getMethod().name());
            logRecord.set(IntegrationLogMeta.ENDPOINT, context.resolveUrl());
        }

        logRecord.set(IntegrationLogMeta.COMPLETED_AT, Instant.now());
        logRecord.set(IntegrationLogMeta.DURATION_MS, Duration.between(
                logRecord.get(IntegrationLogMeta.CREATED_AT),
                logRecord.get(IntegrationLogMeta.COMPLETED_AT)
        ).toMillis());

        logRecord.set(IntegrationLogMeta.STATUS, IntegrationStatus.FAILED.name());
        logRecord.set(IntegrationLogMeta.ERROR_MESSAGE, ex.getMessage());
        if(ex instanceof IntegrationException integrationException) {
            logRecord.set(IntegrationLogMeta.STATUS_CODE, integrationException.getStatusCode());
            logRecord.set(IntegrationLogMeta.ERROR_CODE, integrationException.getErrorCode());
            logRecord.set(IntegrationLogMeta.ERROR_MESSAGE, integrationException.getMessage());
        }

        try {
            logRecord.set(IntegrationLogMeta.REQUEST_PARAM, Utils.O.toJsonString(context.getQueryParams()));
            logRecord.set(IntegrationLogMeta.REQUEST_BODY, Utils.O.toJsonString(context.getBody()));
            logRecord.set(IntegrationLogMeta.STEP_ERROR_MESSAGE, JSONB.valueOf(Utils.O.toJsonString(context.getEvents())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        integrationLogService.createIntegrationLog(logRecord.getValues());
    }


}
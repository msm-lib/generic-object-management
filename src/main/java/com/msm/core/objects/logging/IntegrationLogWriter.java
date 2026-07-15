package com.msm.core.objects.logging;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.IntegrationLogMeta;
import com.msm.core.objects.service.IntegrationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.springframework.scheduling.annotation.Async;

@RequiredArgsConstructor
@Slf4j
public class IntegrationLogWriter {

    private final IntegrationLogService integrationLogService;

    @Async
    public void write(IntegrationLogData data) {
        try {
            DataRecord loggingRecord = DataRecord.of();
            loggingRecord.set(IntegrationLogMeta.CREATED_AT, data.getCreatedAt());
            loggingRecord.set(IntegrationLogMeta.COMPLETED_AT, data.getCompletedAt());
            loggingRecord.set(IntegrationLogMeta.DURATION_MS, data.getDurationMs());
            loggingRecord.set(IntegrationLogMeta.TRACE_ID, data.getTraceId());
            loggingRecord.set(IntegrationLogMeta.SPAN_ID, data.getSpanId());
            loggingRecord.set(IntegrationLogMeta.CORRELATION_ID, data.getCorrelationId());
            loggingRecord.set(IntegrationLogMeta.CONNECTOR, data.getConnector());
            loggingRecord.set(IntegrationLogMeta.OPERATION, data.getOperation());
            loggingRecord.set(IntegrationLogMeta.ENDPOINT, data.getEndpoint());
            loggingRecord.set(IntegrationLogMeta.METHOD, data.getMethod());
            loggingRecord.set(IntegrationLogMeta.STATUS_CODE, data.getStatusCode());
            loggingRecord.set(IntegrationLogMeta.STATUS, data.getStatus() != null ? data.getStatus().name() : null);
            loggingRecord.set(IntegrationLogMeta.TENANT_ID, data.getTenantId());
            loggingRecord.set(IntegrationLogMeta.ENVIRONMENT, data.getEnvironment());
            loggingRecord.set(IntegrationLogMeta.ERROR_CODE, data.getErrorCode());
            loggingRecord.set(IntegrationLogMeta.ERROR_MESSAGE, data.getErrorMessage());
            loggingRecord.set(IntegrationLogMeta.REQUEST_PARAM, data.getRequestParam());
            loggingRecord.set(IntegrationLogMeta.REQUEST_BODY, data.getRequestBody());
            loggingRecord.set(IntegrationLogMeta.RESPONSE_BODY, data.getResponseBody());
            if (data.getErrorDetails() != null) {
                loggingRecord.set(IntegrationLogMeta.STEP_ERROR_MESSAGE, JSONB.valueOf(Utils.O.toJsonString(data.getErrorDetails())));
            }
            integrationLogService.createIntegrationLog(loggingRecord.getValues());
        } catch (Exception e) {
            log.warn("Failed to persist integration log for connector={}, operation={}", data.getConnector(), data.getOperation(), e);
        }
    }
}

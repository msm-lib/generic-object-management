package com.msm.core.objects.logging;

import com.msm.core.objects.entity.enums.IntegrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Snapshot of a single inbound integration call, captured by {@link IntegrationLoggingAspect}
 * and persisted by {@link IntegrationLogWriter}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationLogData {
    String connector;
    String operation;

    String endpoint;
    String method;

    String requestParam;
    String requestBody;
    String responseBody;

    Integer statusCode;
    IntegrationStatus status;

    String errorCode;
    String errorMessage;
    Object errorDetails;

    String tenantId;
    String correlationId;
    String traceId;
    String spanId;
    String environment;

    Instant createdAt;
    Instant completedAt;
    Long durationMs;
    public static class IntegrationLogDataBuilder {}
}

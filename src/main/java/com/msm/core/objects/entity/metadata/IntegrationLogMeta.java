package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.UUID;

import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class IntegrationLogMeta {

    private IntegrationLogMeta() {}

    public static final String OBJECT_NAME = "integrationlog";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("integration_log"));

    // =========================================================
    // Primary
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    // =========================================================
    // Timing
    // =========================================================

    public static final TypedAttribute<Instant> CREATED_AT =
            attr(TABLE, "createdAt", "created_at", Instant.class);

    public static final TypedAttribute<Instant> COMPLETED_AT =
            attr(TABLE, "completedAt", "completed_at", Instant.class);

    public static final TypedAttribute<Long> DURATION_MS =
            attr(TABLE, "durationMs", "duration_ms", Long.class);

    // =========================================================
    // Tracing
    // =========================================================

    public static final TypedAttribute<String> TRACE_ID =
            attr(TABLE, "traceId", "trace_id", String.class);

    public static final TypedAttribute<String> SPAN_ID =
            attr(TABLE, "spanId", "span_id", String.class);

    public static final TypedAttribute<String> CORRELATION_ID =
            attr(TABLE, "correlationId", "correlation_id", String.class);

    // =========================================================
    // Integration
    // =========================================================

    public static final TypedAttribute<String> CONNECTOR =
            attr(TABLE, "connector", "connector", String.class);

    public static final TypedAttribute<String> OPERATION =
            attr(TABLE, "operation", "operation", String.class);

    public static final TypedAttribute<String> ENDPOINT =
            attr(TABLE, "endpoint", "endpoint", String.class);

    public static final TypedAttribute<String> METHOD =
            attr(TABLE, "method", "method", String.class);

    // =========================================================
    // Execution
    // =========================================================

    public static final TypedAttribute<Integer> STATUS_CODE =
            attr(TABLE, "statusCode", "status_code", Integer.class);

    public static final TypedAttribute<String> STATUS =
            attr(TABLE, "status", "status", String.class);

    public static final TypedAttribute<Integer> RETRY_COUNT =
            attr(TABLE, "retryCount", "retry_count", Integer.class);

    public static final TypedAttribute<Long> TIMEOUT_MS =
            attr(TABLE, "timeoutMs", "timeout_ms", Long.class);

    // =========================================================
    // Authentication
    // =========================================================

    public static final TypedAttribute<String> AUTH_PROVIDER =
            attr(TABLE, "authProvider", "auth_provider", String.class);

    // =========================================================
    // Tenant
    // =========================================================

    public static final TypedAttribute<String> TENANT_ID =
            attr(TABLE, "tenantId", "tenant_id", String.class);

    public static final TypedAttribute<String> ENVIRONMENT =
            attr(TABLE, "environment", "environment", String.class);

    // =========================================================
    // Error
    // =========================================================

    public static final TypedAttribute<String> ERROR_CODE =
            attr(TABLE, "errorCode", "error_code", String.class);
    public static final TypedAttribute<String> ERROR_MESSAGE =
            attr(TABLE, "errorMessage", "error_message", String.class);

    public static final TypedAttribute<JSONB> STEP_ERROR_MESSAGE =
            attr(TABLE, "stepErrorMessage", "step_error_message", JSONB.class);

    public static final TypedAttribute<String> REQUEST_PARAM =
            attr(TABLE, "requestParam", "request_param", String.class);

    public static final TypedAttribute<String> REQUEST_BODY =
            attr(TABLE, "requestBody", "request_body", String.class);

    public static final TypedAttribute<String> RESPONSE_BODY =
            attr(TABLE, "responseBody", "response_body", String.class);
}
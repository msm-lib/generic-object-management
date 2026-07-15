package com.msm.core.objects.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

public class LogTraceUtils {
    private static final String TRACE_ID_KEY = "traceId";
    public static final String HEADER_CORRELATION_ID = "x-correlation-id";
    private static final String MDC_KEY_SPAN_ID = "spanId";
    private static final String TRACE_HEADER = "x-kong-request-id";
    private static final String TRACE_TIMESTAMP_KEY = "traceTimestamp";
    private LogTraceUtils() {}

    public static void injectTraceLog(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(TRACE_TIMESTAMP_KEY, Instant.now().toString());
    }

    public static void cleanTraceLog() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(TRACE_TIMESTAMP_KEY);
    }

    public static String getTraceIdLog() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static String getSpanIdLog() {
        return MDC.get(MDC_KEY_SPAN_ID);
    }

    public static String getTraceTimestampLog() {
        return MDC.get(TRACE_TIMESTAMP_KEY);
    }
}

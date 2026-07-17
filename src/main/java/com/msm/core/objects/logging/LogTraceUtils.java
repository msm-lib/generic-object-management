package com.msm.core.objects.logging;

import com.msm.core.commons.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

public class LogTraceUtils {
    public static final String TRACE_ID_KEY = "traceId";
    public static final String HEADER_CORRELATION_ID = "x-correlation-id";
    public static final String MDC_KEY_SPAN_ID = "spanId";
    public static final String KONG_TRACE_ID_HEADER = "x-kong-request-id";
    public static final String TRACE_TIMESTAMP_KEY = "traceTimestamp";
    private LogTraceUtils() {}

    public static void injectTraceLog(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            //accept traceId from KONG gateway
            traceId = request.getHeader(KONG_TRACE_ID_HEADER);
        }


        MDC.put(TRACE_ID_KEY, Utils.STR.defaultIfBlank(traceId, LogTraceUtils::getDefaultTraceId));
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

    private static String getDefaultTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

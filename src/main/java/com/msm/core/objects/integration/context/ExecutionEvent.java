package com.msm.core.objects.integration.context;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ExecutionEvent {

    private Instant timestamp;

    private String component;

    private String componentExecution;

    private String action;

    private String status;

    private String message;

    private String errorType;

    private String stackTrace;
    private Integer attempt;
}
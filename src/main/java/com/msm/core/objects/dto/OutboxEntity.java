package com.msm.core.objects.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEntity {
    public UUID id;
    //Object name
    public String aggregateType;
    //Same event type
    public String aggregateSubtype;

    //Object id
    public UUID aggregateId;

    //Sent to
    public String destination;

    public Map<String, Object> payload;

    //PENDING, PROCESSING, COMPLETED, FAILED
    public String status;
    public Integer retryCount;
    public String responseMessage;
    public Integer httpStatus;
    public Instant createdAt;
    public Instant updatedAt;
}

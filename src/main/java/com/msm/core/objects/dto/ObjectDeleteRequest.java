package com.msm.core.objects.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ObjectDeleteRequest {
    private UUID id;
    private Long version;
}

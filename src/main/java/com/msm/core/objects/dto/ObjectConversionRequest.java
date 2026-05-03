package com.msm.core.objects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ObjectConversionRequest {
    @NotNull
    private String sourceObject;
    @NotNull
    private String targetObject;
    private Map<String, Object> srcData;
}

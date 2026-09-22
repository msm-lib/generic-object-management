package com.msm.core.objects.imports.model;

import com.msm.core.metadata.Attribute;

import java.util.Map;
import java.util.UUID;


public record CellMappingContext(
        UUID jobId,
        String objectName,
        Attribute attribute,
        Map<Integer, String> headerColumn,
        Map<String, Object> rowData
){

    public static CellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<Integer, String> headerColumn, Map<String, Object> rowData) {
        return new CellMappingContext(jobId, objectName, attribute, headerColumn, rowData);
    }

    public static CellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<String, Object> rowData) {
        return new CellMappingContext(jobId, objectName, attribute, Map.of(), rowData);
    }
}

package com.msm.core.objects.imports.model;

import com.msm.core.metadata.Attribute;

import java.util.Map;
import java.util.UUID;


public record CellMapperContext(
        UUID importId,
        String objectName,
        Attribute attribute,
        Map<Integer, String> columnHeader,
        Map<String, Object> rowData
){

    public static CellMapperContext of(UUID importId, String objectName, Attribute attribute, Map<Integer, String> columnHeader, Map<String, Object> rowData) {
        return new CellMapperContext(importId, objectName, attribute, columnHeader, rowData);
    }

    public static CellMapperContext of(UUID importId, String objectName, Attribute attribute, Map<String, Object> rowData) {
        return new CellMapperContext(importId, objectName, attribute, Map.of(), rowData);
    }
}

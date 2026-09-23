package com.msm.core.objects.imports.model;

import com.msm.core.metadata.Attribute;

import java.util.Map;
import java.util.UUID;


public record ExportCellMappingContext(
        UUID jobId,
        String objectName,
        Attribute attribute,
        Map<Integer, ColumnHeaderDefinitionPath> headerColumn,
        Map<String, Object> rowData
){

    public static ExportCellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<Integer, ColumnHeaderDefinitionPath> headerColumn, Map<String, Object> rowData) {
        return new ExportCellMappingContext(jobId, objectName, attribute, headerColumn, rowData);
    }

    public static ExportCellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<String, Object> rowData) {
        return new ExportCellMappingContext(jobId, objectName, attribute, Map.of(), rowData);
    }
}

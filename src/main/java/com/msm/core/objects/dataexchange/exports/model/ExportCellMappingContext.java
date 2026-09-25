package com.msm.core.objects.dataexchange.exports.model;

import com.msm.core.metadata.Attribute;

import java.util.Map;
import java.util.UUID;


public record ExportCellMappingContext(
        UUID jobId,
        String objectName,
        Attribute attribute,
        Map<Integer, ColumnHeaderPath> headerColumn,
        Map<String, Object> rowData
){

    public static ExportCellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<Integer, ColumnHeaderPath> headerColumn, Map<String, Object> rowData) {
        return new ExportCellMappingContext(jobId, objectName, attribute, headerColumn, rowData);
    }

    public static ExportCellMappingContext of(UUID jobId, String objectName, Attribute attribute, Map<String, Object> rowData) {
        return new ExportCellMappingContext(jobId, objectName, attribute, Map.of(), rowData);
    }
}

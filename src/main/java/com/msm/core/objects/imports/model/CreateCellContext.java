package com.msm.core.objects.imports.model;

import org.apache.poi.ss.usermodel.Row;

import java.util.Map;
import java.util.UUID;


public record CreateCellContext(
        UUID jobId,
        String objectName,
        Map<Integer, String> headerColumn,
        Map<String, Object> rowData,
        Row row,
        Object attributeValue
){
    public static CreateCellContext of(UUID jobId, String objectName, Map<Integer, String> headerColumn, Map<String, Object> rowData, Row row, Object attributeValue){
        return new CreateCellContext(jobId, objectName, headerColumn, rowData, row, attributeValue);
    }
}

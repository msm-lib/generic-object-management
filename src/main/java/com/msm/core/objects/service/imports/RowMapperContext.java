package com.msm.core.objects.service.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowMapperContext {
    private String objectName;
    private CSVRecord row;
//    private Map<Attribute, Set<String>> attrCodeMap;
//    private Map<Attribute, Set<String>> attrTypeMap;
//    private Map<Attribute, Map<String, Set<String>>> lookupAttrMap;
}

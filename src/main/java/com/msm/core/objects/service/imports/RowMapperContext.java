package com.msm.core.objects.service.imports;

import com.msm.core.metadata.Attribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVRecord;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowMapperContext {
    private String objectName;
    private CSVRecord row;
    private Map<Attribute, Set<String>> attrCodeMap;
}

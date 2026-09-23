package com.msm.core.objects.imports.excel;

import com.msm.core.commons.Utils;
import com.msm.core.objects.imports.model.ColumnHeaderDefinitionPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelHeaderPathParser {
    private static final Pattern REF_PATTERN = Pattern.compile("^([^.]+)\\.([^.]+)$");


    public static Map<String, ColumnHeaderDefinitionPath> cacheHeaderPaths(List<String> headers) {
        Map<String, ColumnHeaderDefinitionPath> cache = new HashMap<>();

        for (String header : headers) {
            if (Utils.STR.isBlank(header)) continue;

            String cleanHeader = header.strip();
            Matcher matcher = REF_PATTERN.matcher(cleanHeader);

            ColumnHeaderDefinitionPath path;
            if (matcher.matches()) {
                path = ColumnHeaderDefinitionPath.of(true, matcher.group(1), cleanHeader);
            } else {
                path = ColumnHeaderDefinitionPath.of(false, null, cleanHeader);
            }

            cache.put(cleanHeader, path);
        }

        return cache;
    }


    public static ColumnHeaderDefinitionPath parse(String headerValue) {
        if (Utils.STR.isBlank(headerValue)) {
            return ColumnHeaderDefinitionPath.of(false, null, headerValue);
        }

        String cleanHeader = Utils.STR.toCamelCaseUnderscore(headerValue.strip());
        Matcher matcher = REF_PATTERN.matcher(cleanHeader);

        if (matcher.matches()) {
            // Group 1: customerIdReference
            String referenceFieldName = matcher.group(1);
            // Group 2: accountName
//            String nestedAttributeName = matcher.group(2);
            Utils.STR.toCamelCaseUnderscore(referenceFieldName);
            return ColumnHeaderDefinitionPath.of(
                    true,
                    referenceFieldName,
                    cleanHeader
            );
        }

        return ColumnHeaderDefinitionPath.of(false, null, cleanHeader);
    }
}

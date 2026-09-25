package com.msm.core.objects.dataexchange.exports.excel;

import com.msm.core.commons.Utils;
import com.msm.core.objects.dataexchange.exports.model.ColumnHeaderPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelHeaderPathParser {
    private static final Pattern REF_PATTERN = Pattern.compile("^([^.]+)\\.([^.]+)$");


    public static Map<String, ColumnHeaderPath> cacheHeaderPaths(List<String> headers) {
        Map<String, ColumnHeaderPath> cache = new HashMap<>();

        for (String header : headers) {
            if (Utils.STR.isBlank(header)) continue;

            String cleanHeader = header.strip();
            Matcher matcher = REF_PATTERN.matcher(cleanHeader);

            ColumnHeaderPath path;
            if (matcher.matches()) {
                path = ColumnHeaderPath.of(true, matcher.group(1), cleanHeader);
            } else {
                path = ColumnHeaderPath.of(false, null, cleanHeader);
            }

            cache.put(cleanHeader, path);
        }

        return cache;
    }


    public static ColumnHeaderPath parse(String headerValue) {
        if (Utils.STR.isBlank(headerValue)) {
            return ColumnHeaderPath.of(false, null, headerValue);
        }

        String cleanHeader = Utils.STR.toCamelCaseUnderscore(headerValue.strip());
        Matcher matcher = REF_PATTERN.matcher(cleanHeader);

        if (matcher.matches()) {
            // Group 1: customerIdReference
            String referenceFieldName = matcher.group(1);
            // Group 2: accountName
//            String nestedAttributeName = matcher.group(2);
            Utils.STR.toCamelCaseUnderscore(referenceFieldName);
            return ColumnHeaderPath.of(
                    true,
                    referenceFieldName,
                    cleanHeader
            );
        }

        return ColumnHeaderPath.of(false, null, cleanHeader);
    }
}

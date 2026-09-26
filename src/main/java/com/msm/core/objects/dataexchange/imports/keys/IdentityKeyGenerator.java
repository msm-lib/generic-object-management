package com.msm.core.objects.dataexchange.imports.keys;

import com.msm.core.objects.dataexchange.imports.model.IdentityKey;
import com.msm.core.objects.exception.ImportValidationException;

import java.util.List;
import java.util.Map;

public final class IdentityKeyGenerator {

    public static IdentityKey generate(Map<String, Object> data, List<String> fields) {
        return generate(data, fields, "|");
    }

    public static IdentityKey generate(Map<String, Object> data, List<String> fields, String delimiter) {
        StringBuilder key = new StringBuilder();
        int level = 0;
        for (String field : fields) {
            Object value = data.get(field);

            if (isNullOrBlank(value)) {
                break;
            }

            if (!key.isEmpty()) {
                key.append(delimiter);
            }

            String normalized = normalize(value);

            key
//                    .append(field)
//                    .append(':')
//                    .append(normalized.length())
//                    .append(':')
                    .append(normalized);

            level++;
        }

        if (level == 0) {
            throw new ImportValidationException("Identity cannot be empty");
        }

        return new IdentityKey(key.toString(), level);
    }


    public static String generateKey(Map<String, Object> data, List<String> fields, String delimiter) {
        StringBuilder key = new StringBuilder();
        for (String field : fields) {
            Object value = data.get(field);

            if (isNullOrBlank(value)) {
                break;
            }

            if (!key.isEmpty()) {
                key.append(delimiter);
            }

            String normalized = normalize(value);

            key.append(normalized);
        }

        return key.toString();
    }

    private static boolean isNullOrBlank(Object value) {
        return value == null || value instanceof String s && s.isBlank();
    }

    private static String normalize(Object value) {
        return String.valueOf(value).trim();
    }
}

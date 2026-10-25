package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Field by field comparison of two states of a row, in the field-name form the repository hands out (custom values
 * already spread into the row). Fields of the metadata come first, in their order, then the other keys by name.
 */
public class ChangeLogDiff {

    /** Every field whose value differs; a field going from {@code null} to a value counts. */
    public List<ChangedField> diff(ObjectMetadata meta, Map<String, Object> before, Map<String, Object> after, Set<String> ignore) {
        List<ChangedField> changes = new ArrayList<>();
        for (String field : fields(meta, before, after, ignore)) {
            Attribute attribute = meta.getAttributeByName(field);
            Object oldValue = value(attribute, before.get(field));
            Object newValue = value(attribute, after.get(field));
            if (!same(oldValue, newValue)) {
                changes.add(new ChangedField(field, oldValue, newValue, dataType(attribute, oldValue != null ? oldValue : newValue)));
            }
        }
        return changes;
    }

    /** The values of a new row: every field set, before {@code null}. */
    public List<ChangedField> created(ObjectMetadata meta, Map<String, Object> after, Set<String> ignore) {
        return snapshot(meta, after, ignore, false);
    }

    /** The values of a deleted row: every field set, after {@code null}. */
    public List<ChangedField> deleted(ObjectMetadata meta, Map<String, Object> before, Set<String> ignore) {
        return snapshot(meta, before, ignore, true);
    }

    /**
     * Copy of the maps and lists of a row, so a later change of the row (the repository merges the payload into the row it
     * read) does not change the copy. Values (String, Number, Instant, UUID…) are kept as they are.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> copy(Map<String, Object> row) {
        return row == null ? null : (Map<String, Object>) copyValue(row);
    }

    private List<ChangedField> snapshot(ObjectMetadata meta, Map<String, Object> row, Set<String> ignore, boolean deleted) {
        List<ChangedField> fields = new ArrayList<>();
        for (String field : fields(meta, row, Map.of(), ignore)) {
            Attribute attribute = meta.getAttributeByName(field);
            Object value = value(attribute, row.get(field));
            if (value != null) {
                String dataType = dataType(attribute, value);
                fields.add(deleted ? new ChangedField(field, value, null, dataType) : new ChangedField(field, null, value, dataType));
            }
        }
        return fields;
    }

    private Set<String> fields(ObjectMetadata meta, Map<String, Object> before, Map<String, Object> after, Set<String> ignore) {
        Set<String> fields = new LinkedHashSet<>();
        for (Attribute attribute : meta.getAttributes() == null ? List.<Attribute>of() : meta.getAttributes()) {
            fields.add(attribute.getFieldName());
        }
        Set<String> others = new TreeSet<>(before.keySet());
        others.addAll(after.keySet());
        fields.addAll(others);
        fields.removeAll(ignore);
        return fields;
    }

    /** The value cast to the field type, so 1 and 1L, or a date as text and as Instant, compare equal. */
    private Object value(Attribute attribute, Object value) {
        if (attribute == null || value == null) {
            return value;
        }
        try {
            return attribute.cast(value);
        } catch (RuntimeException e) {
            return value;
        }
    }

    private boolean same(Object oldValue, Object newValue) {
        return Objects.equals(normalize(oldValue), normalize(newValue));
    }

    private Object normalize(Object value) {
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros();
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros();
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof OffsetDateTime time) {
            return time.toInstant();
        }
        if (value instanceof ZonedDateTime time) {
            return time.toInstant();
        }
        if (value instanceof Timestamp time) {
            return time.toInstant();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new HashMap<>();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), normalize(item)));
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::normalize).toList();
        }
        if (value instanceof Object[] array) {
            return Arrays.stream(array).map(this::normalize).toList();
        }
        return value;
    }

    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), copyValue(item)));
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> copy = new ArrayList<>(collection.size());
            collection.forEach(item -> copy.add(copyValue(item)));
            return copy;
        }
        return value;
    }

    /** Data type of the notification audit log for scalar values; {@code null} for objects and lists. */
    private String dataType(Attribute attribute, Object value) {
        Class<?> type = attributeType(attribute);
        if (type == null || type == Object.class) {
            type = value == null ? null : value.getClass();
        }
        if (type == null) {
            return null;
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return "number";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        }
        if (type == LocalDate.class) {
            return "date";
        }
        if (Instant.class == type || OffsetDateTime.class == type || ZonedDateTime.class == type
                || LocalDateTime.class == type || Date.class.isAssignableFrom(type)) {
            return "datetime";
        }
        if (CharSequence.class.isAssignableFrom(type) || UUID.class == type || type.isEnum()) {
            return "text";
        }
        return null;
    }

    private Class<?> attributeType(Attribute attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return attribute.getJavaType().getRawClass();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

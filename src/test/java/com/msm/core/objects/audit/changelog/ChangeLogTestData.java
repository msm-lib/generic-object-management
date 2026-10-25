package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChangeLogTestData {

    static final String OBJECT = "testorder";

    private ChangeLogTestData() {
    }

    static ObjectMetadata metadata() {
        return ObjectMetadata.builder()
                .name(OBJECT)
                .tableName("test_order")
                .attributes(List.of(attribute("id", "UUID"), attribute("code", "String"), attribute("note", "String"),
                        attribute("customerId", "UUID"), attribute("quantity", "BigDecimal"), attribute("isDeleted", "Boolean"),
                        attribute("version", "Long"), attribute("updatedAt", "Instant"), attribute("password", "String")))
                .build();
    }

    /** Change log on for testorder, with parent field customerId, as written in application.yml. */
    static ChangeLogProperties properties(Map<String, String> extra) {
        Map<String, String> props = new HashMap<>(Map.of(
                "objects.change-log.enabled", "true",
                "objects.change-log.source-service", "ORDER_SERVICE",
                "objects.change-log.default-tenant", "bhc",
                "objects.change-log.objects[0]", OBJECT,
                "objects.change-log.object-options." + OBJECT + ".parent-field", "customerId",
                "objects.change-log.mask-fields[0]", "password"));
        props.putAll(extra);
        return new Binder(new MapConfigurationPropertySource(props)).bindOrCreate("objects.change-log", ChangeLogProperties.class);
    }

    static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static Attribute attribute(String fieldName, String fieldType) {
        return Attribute.builder().fieldName(fieldName).columnName(fieldName).fieldType(fieldType).build();
    }
}

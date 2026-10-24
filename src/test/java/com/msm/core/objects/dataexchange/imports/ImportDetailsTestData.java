package com.msm.core.objects.dataexchange.imports;

import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.AttributeRef;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.config.ObjectImportRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A header ({@code testorder}) and its detail ({@code testorderitem}) for the import-into-a-header tests. */
final class ImportDetailsTestData {
    static final String HEADER = "testorder";
    static final String DETAIL = "testorderitem";

    private ImportDetailsTestData() {
    }

    static void registerMetadata() {
        ObjectMetadataFactory.registerObjectMetadata(ObjectMetadata.builder()
                .name(HEADER)
                .tableName("test_order")
                .attributes(List.of(attribute("id", "UUID"), attribute("code", "String"), attribute("name", "String"),
                        attribute("version", "Long")))
                .build());
        AttributeRef orderRef = new AttributeRef("orderIdReference", HEADER);
        Attribute orderId = attribute("orderId", "UUID");
        orderId.setAttributeRef(orderRef);
        ObjectMetadataFactory.registerObjectMetadata(ObjectMetadata.builder()
                .name(DETAIL)
                .tableName("test_order_item")
                .attributes(List.of(attribute("id", "UUID"), orderId, attribute("productId", "UUID"), attribute("unitId", "UUID"),
                        attribute("quantity", "BigDecimal"), attribute("version", "Long")))
                .build());
    }

    /** Import config of the detail: parent testorder by orderId, identity product + unit, modes APPEND and UPSERT. */
    static ImportConfigService config(Map<String, String> extra) {
        Map<String, String> props = new HashMap<>(Map.of(
                "imports.objects." + DETAIL + ".parent.object", HEADER,
                "imports.objects." + DETAIL + ".parent.field", "orderId",
                "imports.objects." + DETAIL + ".parent.identity[0]", "productId",
                "imports.objects." + DETAIL + ".parent.identity[1]", "unitId",
                "imports.objects." + DETAIL + ".parent.modes[0]", "APPEND",
                "imports.objects." + DETAIL + ".parent.modes[1]", "UPSERT"));
        props.putAll(extra);
        ObjectImportRegistry registry = new Binder(new MapConfigurationPropertySource(props))
                .bindOrCreate("imports", ObjectImportRegistry.class);
        return new ImportConfigService(registry);
    }

    private static Attribute attribute(String fieldName, String fieldType) {
        return Attribute.builder().fieldName(fieldName).columnName(fieldName).fieldType(fieldType).build();
    }
}

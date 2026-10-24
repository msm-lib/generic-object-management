package com.msm.core.objects.dataexchange.imports;

import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.objects.dataexchange.imports.ImportDetailsTestData.DETAIL;
import static org.assertj.core.api.Assertions.assertThat;

class ImportParentRowsTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final ImportContext CONTEXT = new ImportContext("testorder", ORDER_ID, "DH.0001", 3L, ImportMode.APPEND,
            Map.of("id", ORDER_ID, "code", "DH.0001", "name", "Đơn 1"));

    private final ImportParentRows rows = new ImportParentRows();
    private final ImportConfigService config = ImportDetailsTestData.config(Map.of());

    @BeforeAll
    static void metadata() {
        ImportDetailsTestData.registerMetadata();
    }

    @Test
    void rowWithoutHeaderColumnGetsTheHeaderAndItsSnapshot() {
        Map<String, Object> row = new HashMap<>(Map.of("quantity", 2));

        String error = fill(row);

        assertThat(error).isNull();
        assertThat(row.get("orderId")).isEqualTo(ORDER_ID);
        assertThat(row.get("orderIdReference")).isEqualTo(Map.of("id", ORDER_ID, "code", "DH.0001", "name", "Đơn 1"));
    }

    @Test
    void headerColumnWithTheSameHeaderIsAccepted() {
        Map<String, Object> byCode = new HashMap<>(Map.of("orderId", "DH.0001"));
        Map<String, Object> byId = new HashMap<>(Map.of("orderId", ORDER_ID.toString().toUpperCase()));

        assertThat(fill(byCode)).isNull();
        assertThat(fill(byId)).isNull();
        assertThat(byCode.get("orderId")).isEqualTo(ORDER_ID);
    }

    @Test
    void headerColumnWithAnotherHeaderIsAnError() {
        Map<String, Object> row = new HashMap<>(Map.of("orderId", "DH.0002"));

        String error = fill(row);

        assertThat(error).contains("orderId").contains("DH.0002").contains("DH.0001");
        assertThat(row.get("orderId")).isEqualTo("DH.0002");
    }

    private String fill(Map<String, Object> row) {
        return rows.fill(row, CONTEXT, config.getParent(DETAIL), ObjectMetadataFactory.getObjectMetadataByName(DETAIL));
    }
}

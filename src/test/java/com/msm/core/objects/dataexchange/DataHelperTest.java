package com.msm.core.objects.dataexchange;

import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataHelperTest {

    private static final ObjectMetadata ALLOCATION = ObjectMetadata.builder()
            .name("testallocation")
            .tableName("test_allocation")
            .attributes(List.of(attribute("headerId"), attribute("subjectId"), attribute("organizationId")))
            .build();

    @Test
    void headerOfAttributeColumnsIsValid() {
        assertThat(DataHelper.isColumnHeaderValid(ALLOCATION, Map.of(0, "headerId", 1, "subjectId"), 4)).isTrue();
        assertThat(DataHelper.isColumnHeaderValid(ALLOCATION, Map.of(0, "headerId"), 4)).isFalse();
    }

    @Test
    void columnsMappedByTheServiceCountTowardsTheHeader() {
        // 3 attribute columns + 5 dynamic target columns mapped by the service's own header handler
        Map<Integer, String> header = Map.of(0, "headerId", 1, "subjectId", 2, "organizationId",
                3, "_target:K1", 4, "_target:K2", 5, "_target:K3", 6, "_target:K4", 7, "_target:K5");

        assertThat(DataHelper.isColumnHeaderValid(ALLOCATION, header, 8)).isTrue();
    }

    @Test
    void headerWithoutAnyAttributeIsNotValid() {
        assertThat(DataHelper.isColumnHeaderValid(ALLOCATION, Map.of(0, "_target:K1", 1, "_target:K2"), 2)).isFalse();
        assertThat(DataHelper.isColumnHeaderValid(ALLOCATION, Map.of(), 2)).isFalse();
    }

    private static Attribute attribute(String fieldName) {
        return Attribute.builder().fieldName(fieldName).columnName(fieldName).fieldType("UUID").build();
    }
}

package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.ObjectMetadata;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.msm.core.objects.audit.changelog.ChangeLogTestData.row;
import static org.assertj.core.api.Assertions.assertThat;

class ChangeLogDiffTest {

    private final ChangeLogDiff diff = new ChangeLogDiff();
    private final ObjectMetadata meta = ChangeLogTestData.metadata();

    @Test
    void fieldSetForTheFirstTimeIsAChange() {
        List<ChangedField> changes = diff.diff(meta, row("id", "1", "note", null), row("id", "1", "note", "giao gấp"), Set.of());

        assertThat(changes).containsExactly(new ChangedField("note", null, "giao gấp", "text"));
    }

    @Test
    void sameNumberWithAnotherScaleOrTypeIsNoChange() {
        List<ChangedField> changes = diff.diff(meta,
                row("quantity", new BigDecimal("10.000"), "extra", Map.of("n", 1)),
                row("quantity", 10, "extra", Map.of("n", 1L)), Set.of());

        assertThat(changes).isEmpty();
    }

    @Test
    void ignoredFieldsAreNotCompared() {
        List<ChangedField> changes = diff.diff(meta, row("version", 1L, "note", "a"), row("version", 2L, "note", "b"), Set.of("version"));

        assertThat(changes).extracting(ChangedField::name).containsExactly("note");
    }

    @Test
    void fieldsOutsideTheMetadataComeAfterItsFields() {
        List<ChangedField> changes = diff.diff(meta,
                row("zone", "A", "note", "a", "customerIdReference", Map.of("code", "KH1")),
                row("zone", "B", "note", "b", "customerIdReference", Map.of("code", "KH2")), Set.of());

        assertThat(changes).extracting(ChangedField::name).containsExactly("note", "customerIdReference", "zone");
        assertThat(changes.get(1).dataType()).isNull();
    }

    @Test
    void createdAndDeletedListTheFieldsSet() {
        UUID id = UUID.randomUUID();
        Map<String, Object> state = row("id", id.toString(), "code", "DH1", "note", null, "version", 0L);

        assertThat(diff.created(meta, state, Set.of("version")))
                .containsExactly(new ChangedField("id", null, id, "text"), new ChangedField("code", null, "DH1", "text"));
        assertThat(diff.deleted(meta, state, Set.of("version"))).extracting(ChangedField::after).containsOnlyNulls();
    }

    @Test
    @SuppressWarnings("unchecked")
    void copyIsNotChangedByALaterChangeOfTheRow() {
        Map<String, Object> state = row("note", "a", "items", new ArrayList<>(List.of("x")));
        Map<String, Object> copy = diff.copy(state);

        state.put("note", "b");
        ((List<Object>) state.get("items")).add("y");

        assertThat(copy).containsEntry("note", "a").containsEntry("items", List.of("x"));
    }
}

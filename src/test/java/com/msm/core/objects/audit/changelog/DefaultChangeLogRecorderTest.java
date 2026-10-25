package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.ObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.objects.audit.changelog.ChangeLogTestData.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultChangeLogRecorderTest {

    private final ObjectMetadata meta = ChangeLogTestData.metadata();
    private final ChangeLogCollector collector = mock(ChangeLogCollector.class);
    private final String id = UUID.randomUUID().toString();
    private final String customerId = UUID.randomUUID().toString();

    @BeforeEach
    void txId() {
        when(collector.txId()).thenReturn("tx-1");
    }

    private DefaultChangeLogRecorder recorder(Map<String, String> extra) {
        return new DefaultChangeLogRecorder(ChangeLogTestData.properties(extra), collector, new ChangeLogDiff());
    }

    @SuppressWarnings("unchecked")
    private List<ChangeLogEntry> recorded() {
        ArgumentCaptor<Collection<ChangeLogEntry>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(collector).add(captor.capture());
        return new ArrayList<>(captor.getValue());
    }

    @Test
    void objectWithoutChangeLogIsNotRecorded() {
        DefaultChangeLogRecorder recorder = recorder(Map.of());
        ObjectMetadata other = ObjectMetadata.builder().name("v2order").attributes(List.of()).build();

        assertThat(recorder.isEnabled(other)).isFalse();
        recorder.recordCreate(other, List.of(row("id", id)));
        verify(collector, never()).add(any());
    }

    @Test
    void masterSwitchOffRecordsNothing() {
        assertThat(recorder(Map.of("objects.change-log.enabled", "false")).isEnabled(meta)).isFalse();
    }

    @Test
    void updateGivesTheChangedFieldsOnly() {
        recorder(Map.of()).recordUpdate(meta,
                List.of(row("id", id, "code", "DH1", "note", "a", "customerId", customerId, "version", 1L)),
                List.of(row("id", id, "code", "DH1", "note", "b", "customerId", customerId, "version", 2L)));

        ChangeLogEntry entry = recorded().get(0);
        assertThat(entry.action()).isEqualTo("UPDATE");
        assertThat(entry.entityType()).isEqualTo("testorder");
        assertThat(entry.entityId()).isEqualTo(id);
        assertThat(entry.entityCode()).isEqualTo("DH1");
        assertThat(entry.entityParentId()).isEqualTo(customerId);
        assertThat(entry.sourceService()).isEqualTo("ORDER_SERVICE");
        assertThat(entry.changedFields()).containsExactly(new ChangedField("note", "a", "b", "text"));
    }

    @Test
    void rowWithoutChangeIsSkipped() {
        recorder(Map.of()).recordUpdate(meta, List.of(row("id", id, "note", "a", "version", 1L)),
                List.of(row("id", id, "note", "a", "version", 2L)));

        assertThat(recorded()).isEmpty();
    }

    @Test
    void afterWithoutBeforeIsACreate() {
        recorder(Map.of()).recordUpdate(meta, List.of(), List.of(row("id", id, "code", "DH2")));

        assertThat(recorded()).extracting(ChangeLogEntry::action).containsExactly("CREATE");
    }

    @Test
    void isDeletedGoingToTrueIsASoftDelete() {
        recorder(Map.of()).recordUpdate(meta, List.of(row("id", id, "isDeleted", false)), List.of(row("id", id, "isDeleted", true)));

        ChangeLogEntry entry = recorded().get(0);
        assertThat(entry.action()).isEqualTo("DELETE");
        assertThat(entry.metadata()).containsEntry("deleteType", "SOFT");
    }

    @Test
    void hardDeleteKeepsTheValuesOfTheRow() {
        recorder(Map.of()).recordDelete(meta, List.of(row("id", id, "code", "DH1")), false);

        ChangeLogEntry entry = recorded().get(0);
        assertThat(entry.metadata()).containsEntry("deleteType", "HARD");
        assertThat(entry.changedFields()).extracting(ChangedField::before).contains("DH1");
    }

    @Test
    void maskedAndLongValuesAreSanitized() {
        recorder(Map.of("objects.change-log.max-value-length", "5")).recordUpdate(meta,
                List.of(row("id", id, "password", "old", "note", "abc")),
                List.of(row("id", id, "password", "new", "note", "abcdefgh")));

        assertThat(recorded().get(0).changedFields()).containsExactly(
                new ChangedField("note", "abc", "abcde" + DefaultChangeLogRecorder.TRUNCATED, "text"),
                new ChangedField("password", DefaultChangeLogRecorder.MASK, DefaultChangeLogRecorder.MASK, "text"));
    }

    @Test
    void withoutUserInTheContextTheActorIsTheSystem() {
        recorder(Map.of()).recordCreate(meta, List.of(row("id", id)));

        ChangeLogEntry entry = recorded().get(0);
        assertThat(entry.actor().type()).isEqualTo("SYSTEM");
        assertThat(entry.tenantCode()).isEqualTo("bhc");
        assertThat(entry.txId()).isEqualTo("tx-1");
    }
}

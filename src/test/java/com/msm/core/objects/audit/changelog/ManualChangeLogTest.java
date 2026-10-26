package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.service.GenericObjectMetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.msm.core.objects.audit.changelog.ChangeLogTestData.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManualChangeLogTest {

    private final ObjectMetadata meta = ChangeLogTestData.metadata();
    private final ChangeLogCollector collector = mock(ChangeLogCollector.class);
    private final GenericObjectMetadataService metadataService = mock(GenericObjectMetadataService.class);
    private final ManualChangeLog manualChangeLog = new ManualChangeLog(
            new DefaultChangeLogRecorder(ChangeLogTestData.properties(Map.of()), collector, new ChangeLogDiff()), metadataService);
    private final String id = UUID.randomUUID().toString();
    private final String customerId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(collector.txId()).thenReturn("tx-1");
        when(metadataService.getObjectMetadata(ChangeLogTestData.OBJECT)).thenReturn(Optional.of(meta));
    }

    @SuppressWarnings("unchecked")
    private List<ChangeLogEntry> recorded() {
        ArgumentCaptor<Collection<ChangeLogEntry>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(collector).add(captor.capture());
        return new ArrayList<>(captor.getValue());
    }

    @Test
    void updateRecordsOnlyTheChangedFieldsOfAPartialRow() {
        manualChangeLog.update(ChangeLogTestData.OBJECT,
                row("id", id, "code", "O-1", "customerId", customerId, "note", "old", "quantity", new BigDecimal("10")),
                row("id", id, "code", "O-1", "customerId", customerId, "note", "new", "quantity", new BigDecimal("10.000")));

        List<ChangeLogEntry> entries = recorded();
        assertThat(entries).hasSize(1);
        ChangeLogEntry entry = entries.getFirst();
        assertThat(entry.action()).isEqualTo(ChangeLogEntry.ACTION_UPDATE);
        assertThat(entry.entityId()).isEqualTo(id);
        assertThat(entry.entityCode()).isEqualTo("O-1");
        assertThat(entry.entityParentId()).isEqualTo(customerId);
        assertThat(entry.changedFields()).containsExactly(new ChangedField("note", "old", "new", "text"));
    }

    @Test
    void updateWithoutChangeRecordsNothing() {
        manualChangeLog.update(ChangeLogTestData.OBJECT, row("id", id, "note", "same"), row("id", id, "note", "same"));

        assertThat(recorded()).isEmpty();
    }

    @Test
    void createAndDeleteCarryTheRowValues() {
        manualChangeLog.create(ChangeLogTestData.OBJECT, row("id", id, "code", "O-1", "note", "n"));
        ChangeLogEntry created = recorded().getFirst();
        assertThat(created.action()).isEqualTo(ChangeLogEntry.ACTION_CREATE);
        assertThat(created.changedFields()).extracting(ChangedField::name).containsExactly("id", "code", "note");

        ChangeLogCollector deleteCollector = mock(ChangeLogCollector.class);
        new ManualChangeLog(new DefaultChangeLogRecorder(ChangeLogTestData.properties(Map.of()), deleteCollector, new ChangeLogDiff()),
                metadataService).delete(ChangeLogTestData.OBJECT, row("id", id, "code", "O-1"), true);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ChangeLogEntry>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(deleteCollector).add(captor.capture());
        ChangeLogEntry deleted = captor.getValue().iterator().next();
        assertThat(deleted.action()).isEqualTo(ChangeLogEntry.ACTION_DELETE);
        assertThat(deleted.metadata()).containsEntry("deleteType", "SOFT");
    }

    @Test
    void objectWithoutChangeLogRecordsNothing() {
        ObjectMetadata other = ObjectMetadata.builder().name("v2order").attributes(List.of()).build();
        when(metadataService.getObjectMetadata("v2order")).thenReturn(Optional.of(other));

        manualChangeLog.update("v2order", row("note", "a"), row("note", "b"));

        verify(collector, never()).add(any());
    }

    @Test
    void unknownObjectIsRefused() {
        when(metadataService.getObjectMetadata("nosuchobject")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manualChangeLog.create("nosuchobject", row("id", id))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rowWithoutIdIsRefused() {
        assertThatThrownBy(() -> manualChangeLog.update(ChangeLogTestData.OBJECT, row("note", "a"), row("note", "b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
        verify(collector, never()).add(any());
    }
}

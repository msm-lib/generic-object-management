package com.msm.core.objects.repository;

import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.changelog.ChangeLogRecorder;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.service.GenericObjectMetadataService;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultObjectQueryRepositoryChangeLogTest {

    private static final String OBJECT = "testorder";

    private final ObjectQuery query = mock(ObjectQuery.class);
    private final ObjectQuery changeLogQuery = mock(ObjectQuery.class);
    private final GenericObjectMetadataService metadataService = mock(GenericObjectMetadataService.class);
    private final ChangeLogRecorder recorder = mock(ChangeLogRecorder.class);
    private final ObjectMetadata meta = ObjectMetadata.builder()
            .name(OBJECT)
            .tableName("test_order")
            .attributes(List.of(attribute("id", "UUID"), attribute("code", "String"), attribute("note", "String"),
                    attribute("isDeleted", "Boolean"), attribute("version", "Long")))
            .build();
    private DefaultObjectQueryRepository repository;

    @BeforeEach
    void setUp() {
        when(metadataService.getObjectMetadata(OBJECT)).thenReturn(Optional.of(meta));
        when(recorder.isEnabled(meta)).thenReturn(true);
        AuditStrategy audit = mock(AuditStrategy.class);
        CustomValueMappingStrategy mapping = mock(CustomValueMappingStrategy.class);
        repository = new DefaultObjectQueryRepository(query, metadataService, input -> audit, input -> mapping, recorder,
                changeLogQuery, null);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<Map<String, Object>>> rows() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Test
    void updateLogsTheWholeRowBeforeAndAfterNotThePayload() {
        UUID id = UUID.randomUUID();
        when(query.findById(meta, id, null)).thenReturn(row("id", id, "code", "DH1", "note", "a", "version", 1L));
        when(query.updateById(eq(meta), eq(id), anyMap())).thenReturn(1);

        repository.update(OBJECT, id, row("note", "b", "version", 1L));

        ArgumentCaptor<List<Map<String, Object>>> befores = rows();
        ArgumentCaptor<List<Map<String, Object>>> afters = rows();
        verify(recorder).recordUpdate(eq(meta), befores.capture(), afters.capture());
        assertThat(befores.getValue().get(0)).containsEntry("note", "a").containsEntry("code", "DH1");
        assertThat(afters.getValue().get(0)).containsEntry("note", "b").containsEntry("code", "DH1");
    }

    @Test
    void updateNotWrittenIsNotLogged() {
        UUID id = UUID.randomUUID();
        when(query.findById(meta, id, null)).thenReturn(row("id", id, "note", "a"));
        when(query.updateById(eq(meta), eq(id), anyMap())).thenReturn(0);

        repository.update(OBJECT, id, row("note", "b"));

        verify(recorder, never()).recordUpdate(any(), anyList(), anyList());
    }

    @Test
    void objectWithoutChangeLogIsNotReadAgain() {
        when(recorder.isEnabled(meta)).thenReturn(false);
        Condition condition = DSL.noCondition();

        repository.updateWithCondition(OBJECT, condition, row("note", "b"));

        verify(query).update(meta, row("note", "b"), condition);
        verifyNoInteractions(changeLogQuery);
    }

    @Test
    void updateByConditionReadsTheRowsBeforeAndAfter() {
        UUID id = UUID.randomUUID();
        Condition condition = DSL.field("code").eq("DH1");
        when(changeLogQuery.findByCondition(meta, condition)).thenReturn(List.of(row("id", id, "note", "a")));
        when(query.update(meta, row("note", "b"), condition)).thenReturn(1);
        when(changeLogQuery.findByIds(meta, List.of(id))).thenReturn(List.of(row("id", id, "note", "b")));

        repository.updateWithCondition(OBJECT, condition, row("note", "b"));

        ArgumentCaptor<List<Map<String, Object>>> befores = rows();
        ArgumentCaptor<List<Map<String, Object>>> afters = rows();
        verify(recorder).recordUpdate(eq(meta), befores.capture(), afters.capture());
        assertThat(befores.getValue()).extracting(row -> row.get("note")).containsExactly("a");
        assertThat(afters.getValue()).extracting(row -> row.get("note")).containsExactly("b");
    }

    @Test
    void deleteByIdsLogsOnlyTheRowsDeleted() {
        UUID deleted = UUID.randomUUID();
        UUID kept = UUID.randomUUID();
        List<Map<String, Object>> payload = List.of(row("id", deleted, "version", 1L), row("id", kept, "version", 9L));
        when(changeLogQuery.findByIds(meta, List.of(deleted, kept)))
                .thenReturn(List.of(row("id", deleted, "isDeleted", false), row("id", kept, "isDeleted", false)))
                .thenReturn(List.of(row("id", deleted, "isDeleted", true), row("id", kept, "isDeleted", false)));
        when(query.delete(meta, payload)).thenReturn(1);

        repository.delete(OBJECT, payload);

        ArgumentCaptor<List<Map<String, Object>>> befores = rows();
        verify(recorder).recordDelete(eq(meta), befores.capture(), eq(true));
        assertThat(befores.getValue()).extracting(row -> row.get("id")).containsExactly(deleted);
    }

    @Test
    void insertBatchGivesAnIdAndLogsTheRowsInserted() {
        List<Map<String, Object>> payload = List.of(row("code", "A"), row("code", "B"));
        when(query.insertBatchIgnoreDuplicate(meta, payload)).thenReturn(new int[]{1, 0});

        repository.insertBatchIgnoreDuplicate(OBJECT, payload);

        assertThat(payload).allSatisfy(row -> assertThat(row.get("id")).isInstanceOf(UUID.class));
        ArgumentCaptor<List<Map<String, Object>>> afters = rows();
        verify(recorder).recordCreate(eq(meta), afters.capture());
        assertThat(afters.getValue()).extracting(row -> row.get("code")).containsExactly("A");
    }

    @Test
    void upsertReadsTheRowWithTheSameKey() {
        UUID id = UUID.randomUUID();
        when(changeLogQuery.findByCondition(eq(meta), any(Condition.class)))
                .thenReturn(List.of(row("id", id, "code", "DH1", "note", "a")))
                .thenReturn(List.of(row("id", id, "code", "DH1", "note", "b")));
        when(query.upsert(eq(meta), anyMap(), eq(List.of("code")), any())).thenReturn(1);

        repository.upsert(OBJECT, row("code", "DH1", "note", "b"), List.of("code"), null);

        ArgumentCaptor<List<Map<String, Object>>> befores = rows();
        ArgumentCaptor<List<Map<String, Object>>> afters = rows();
        verify(recorder).recordUpdate(eq(meta), befores.capture(), afters.capture());
        assertThat(befores.getValue()).extracting(row -> row.get("note")).containsExactly("a");
        assertThat(afters.getValue()).extracting(row -> row.get("note")).containsExactly("b");
    }

    @Test
    void upsertOnAConstraintWithoutKnownColumnsIsRefused() {
        List<Map<String, Object>> rows = new ArrayList<>(List.of(row("code", "DH1")));

        assertThatThrownBy(() -> repository.bulkUpsertReturning(OBJECT, rows, "uk_missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uk_missing");
        verify(query, never()).upsertReturning(any(ObjectMetadata.class), anyList(), anyString());
    }

    @Test
    void upsertOnAConstraintWithoutChangeLogNeedsNoColumns() {
        when(recorder.isEnabled(meta)).thenReturn(false);
        List<Map<String, Object>> rows = new ArrayList<>(List.of(row("code", "DH1")));
        when(query.upsertReturning(meta, rows, "uk_missing")).thenReturn(List.of());

        repository.bulkUpsertReturning(OBJECT, rows, "uk_missing");

        verify(query).upsertReturning(meta, rows, "uk_missing");
    }

    private static Map<String, Object> row(Object... keyValues) {
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

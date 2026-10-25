package com.msm.core.objects.audit.changelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStepN;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Table {@code audit_log_outbox} of the service (created by the service's own Flyway migration, see the README).
 * Written with jOOQ directly, not as a generic object: the outbox is not exposed through the generic routes and its
 * own writes are not logged.
 */
@RequiredArgsConstructor
public class ChangeLogOutboxStore {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    private static final int INSERT_CHUNK = 500;

    private static final Table<Record> TABLE = DSL.table(DSL.name("audit_log_outbox"));
    private static final Field<UUID> ID = DSL.field(DSL.name("id"), SQLDataType.UUID);
    private static final Field<OffsetDateTime> CREATED_AT = DSL.field(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<String> TENANT_CODE = DSL.field(DSL.name("tenant_code"), SQLDataType.VARCHAR);
    private static final Field<String> SOURCE_SERVICE = DSL.field(DSL.name("source_service"), SQLDataType.VARCHAR);
    private static final Field<String> TX_ID = DSL.field(DSL.name("tx_id"), SQLDataType.VARCHAR);
    private static final Field<String> ACTION = DSL.field(DSL.name("action"), SQLDataType.VARCHAR);
    private static final Field<String> ENTITY_TYPE = DSL.field(DSL.name("entity_type"), SQLDataType.VARCHAR);
    private static final Field<String> ENTITY_ID = DSL.field(DSL.name("entity_id"), SQLDataType.VARCHAR);
    private static final Field<String> ENTITY_PARENT_ID = DSL.field(DSL.name("entity_parent_id"), SQLDataType.VARCHAR);
    private static final Field<String> ENTITY_CODE = DSL.field(DSL.name("entity_code"), SQLDataType.VARCHAR);
    private static final Field<JSONB> ACTOR = DSL.field(DSL.name("actor"), SQLDataType.JSONB);
    private static final Field<JSONB> CHANGED_FIELDS = DSL.field(DSL.name("changed_fields"), SQLDataType.JSONB);
    private static final Field<JSONB> METADATA = DSL.field(DSL.name("metadata"), SQLDataType.JSONB);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), SQLDataType.VARCHAR);
    private static final Field<Integer> ATTEMPTS = DSL.field(DSL.name("attempts"), SQLDataType.INTEGER);
    private static final Field<OffsetDateTime> NEXT_ATTEMPT_AT = DSL.field(DSL.name("next_attempt_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> LOCKED_UNTIL = DSL.field(DSL.name("locked_until"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> SENT_AT = DSL.field(DSL.name("sent_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<String> LAST_ERROR = DSL.field(DSL.name("last_error"), SQLDataType.CLOB);

    private static final String CLAIM_SQL = """
            update audit_log_outbox set status = 'SENDING', locked_until = {0}, attempts = attempts + 1
            where id in (
                select id from audit_log_outbox
                where (status = 'PENDING' and next_attempt_at <= now()) or (status = 'SENDING' and locked_until < now())
                order by created_at
                limit {1}
                for update skip locked)
            returning id, created_at, tenant_code, source_service, tx_id, action, entity_type, entity_id, entity_parent_id,
                entity_code, actor::text as actor, changed_fields::text as changed_fields, metadata::text as metadata, attempts
            """;

    private final DSLContext dsl;

    /** Inserts the entries with the connection of the current transaction, if any. */
    public void insert(Collection<ChangeLogEntry> entries) {
        List<ChangeLogEntry> rows = new ArrayList<>(entries);
        for (int from = 0; from < rows.size(); from += INSERT_CHUNK) {
            List<ChangeLogEntry> chunk = rows.subList(from, Math.min(from + INSERT_CHUNK, rows.size()));
            // next_attempt_at keeps its default now(): due by the database clock the claim compares with, not the app's
            InsertValuesStepN<Record> insert = dsl.insertInto(TABLE).columns(List.of(ID, CREATED_AT, TENANT_CODE, SOURCE_SERVICE,
                    TX_ID, ACTION, ENTITY_TYPE, ENTITY_ID, ENTITY_PARENT_ID, ENTITY_CODE, ACTOR, CHANGED_FIELDS, METADATA, STATUS,
                    ATTEMPTS));
            for (ChangeLogEntry entry : chunk) {
                insert = insert.values(entry.id(), time(entry.timestamp()), entry.tenantCode(), entry.sourceService(), entry.txId(),
                        entry.action(), entry.entityType(), entry.entityId(), entry.entityParentId(), entry.entityCode(),
                        json(entry.actor()), json(entry.changedFields()), json(entry.metadata()), STATUS_PENDING, 0);
            }
            insert.execute();
        }
    }

    /**
     * Takes up to {@code limit} entries due for sending and marks them SENDING until {@code lockedUntil}, skipping rows
     * another instance is claiming at the same moment. One statement, so it needs no transaction.
     */
    public List<OutboxRow> claim(int limit, Instant lockedUntil) {
        // typed binds: a plain OffsetDateTime bind reaches Postgres as varchar
        return dsl.resultQuery(CLAIM_SQL, DSL.val(time(lockedUntil), SQLDataType.TIMESTAMPWITHTIMEZONE), DSL.val(limit, SQLDataType.INTEGER))
                .fetch()
                .map(this::toRow);
    }

    public void markSent(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        dsl.update(TABLE)
                .set(STATUS, STATUS_SENT)
                .set(SENT_AT, time(Instant.now()))
                .setNull(LOCKED_UNTIL)
                .setNull(LAST_ERROR)
                .where(ID.in(ids))
                .execute();
    }

    /** Back to PENDING until {@code nextAttemptAt}, or FAILED for good when {@code nextAttemptAt} is null. */
    public void markFailed(UUID id, String error, Instant nextAttemptAt) {
        dsl.update(TABLE)
                .set(STATUS, nextAttemptAt == null ? STATUS_FAILED : STATUS_PENDING)
                .set(NEXT_ATTEMPT_AT, nextAttemptAt == null ? time(Instant.now()) : time(nextAttemptAt))
                .setNull(LOCKED_UNTIL)
                .set(LAST_ERROR, error)
                .where(ID.eq(id))
                .execute();
    }

    public int deleteSentBefore(Instant before) {
        return dsl.deleteFrom(TABLE)
                .where(STATUS.eq(STATUS_SENT))
                .and(SENT_AT.lt(time(before)))
                .execute();
    }

    private OutboxRow toRow(Record record) {
        return new OutboxRow(
                record.get("id", UUID.class),
                record.get("created_at", OffsetDateTime.class).toInstant(),
                record.get("tenant_code", String.class),
                record.get("source_service", String.class),
                record.get("tx_id", String.class),
                record.get("action", String.class),
                record.get("entity_type", String.class),
                record.get("entity_id", String.class),
                record.get("entity_parent_id", String.class),
                record.get("entity_code", String.class),
                parse(record.get("actor", String.class)),
                parse(record.get("changed_fields", String.class)),
                parse(record.get("metadata", String.class)),
                record.get("attempts", Integer.class));
    }

    private Object parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return Utils.O.toObject(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read audit_log_outbox json: " + e.getOriginalMessage(), e);
        }
    }

    private JSONB json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSONB.valueOf(Utils.O.toJsonString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write audit_log_outbox json: " + e.getOriginalMessage(), e);
        }
    }

    private OffsetDateTime time(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    // An entry read back for sending; json columns as maps / lists.
    public record OutboxRow(UUID id, Instant createdAt, String tenantCode, String sourceService, String txId, String action,
                            String entityType, String entityId, String entityParentId, String entityCode, Object actor,
                            Object changedFields, Object metadata, int attempts) {

        public Map<String, Object> auditLog() {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("logCategory", "DATA_CHANGE");
            log.put("action", action);
            log.put("sourceService", sourceService);
            log.put("txId", txId);
            log.put("originalTxId", txId);
            log.put("entityType", entityType);
            log.put("entityId", entityId);
            log.put("entityParentId", entityParentId);
            log.put("entityCode", entityCode);
            log.put("actor", actor);
            log.put("changedFields", changedFields);
            log.put("metadata", metadata);
            log.put("timestamp", createdAt.toString());
            return log;
        }
    }
}

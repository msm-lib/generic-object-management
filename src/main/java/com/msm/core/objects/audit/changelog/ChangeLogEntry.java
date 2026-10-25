package com.msm.core.objects.audit.changelog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One change of one row, shaped as the notification audit log ({@code AuditLogCreateDto}, category DATA_CHANGE).
 *
 * @param id id of the outbox row
 * @param timestamp when the change was recorded
 * @param tenantCode tenant of the request, sent as {@code x-tenant}
 * @param sourceService service that made the change (ORDER_SERVICE…)
 * @param txId trace id of the request, or an id shared by the transaction
 * @param action CREATE, UPDATE or DELETE
 * @param entityType object name
 * @param entityId id of the row
 * @param entityParentId id of the parent row, when the object has a parent field
 * @param entityCode business code of the row
 * @param actor who made the change
 * @param changedFields fields with their values before and after
 * @param metadata extra facts, e.g. {@code deleteType} SOFT / HARD
 */
public record ChangeLogEntry(
        UUID id,
        Instant timestamp,
        String tenantCode,
        String sourceService,
        String txId,
        String action,
        String entityType,
        String entityId,
        String entityParentId,
        String entityCode,
        ChangeLogActor actor,
        List<ChangedField> changedFields,
        Map<String, Object> metadata) {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
}

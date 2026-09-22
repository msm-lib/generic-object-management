package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.commons.GenericTypeResolverFactory.resolve;
import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class ExportJobMeta {

    private ExportJobMeta() {}

    public static final String OBJECT_NAME = "exportjob";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("bhc", "export_job"));

    // =========================================================
    // Primary
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    // =========================================================
    // Core Info
    // =========================================================

    public static final TypedAttribute<String> OBJECT_NAME_FIELD =
            attr(TABLE, "objectName", "object_name", String.class);

    public static final TypedAttribute<String> FILE_NAME =
            attr(TABLE, "fileName", "file_name", String.class);

    public static final TypedAttribute<String> ORIGINAL_FILE_NAME =
            attr(TABLE, "originalFileName", "original_file_name", String.class);

    public static final TypedAttribute<String> SERVICE =
            attr(TABLE, "service", "service", String.class);

    public static final TypedAttribute<String> STATUS =
            attr(TABLE, "status", "status", String.class);

    public static final TypedAttribute<Map<String, Object>> FILTER_CRITERIA =
            attr(TABLE, "filterCriteria", "filter_criteria", resolve(Map.class, String.class, Object.class));

    // =========================================================
    // Metrics & Errors
    // =========================================================

    public static final TypedAttribute<Long> TOTAL_ROWS =
            attr(TABLE, "totalRows", "total_rows", Long.class);

    public static final TypedAttribute<String> ERROR_MESSAGE =
            attr(TABLE, "errorMessage", "error_message", String.class);

    // =========================================================
    // Timing
    // =========================================================

    public static final TypedAttribute<Instant> STARTED_AT =
            attr(TABLE, "startedAt", "started_at", Instant.class);

    public static final TypedAttribute<Instant> COMPLETED_AT =
            attr(TABLE, "completedAt", "completed_at", Instant.class);

    public static final TypedAttribute<Map<String, Object>> FILE_INFO =
            attr(TABLE, "fileInfo", "file_info", resolve(Map.class, String.class, Object.class));

    // =========================================================
    // Auditing & Lifecycle
    // =========================================================

    public static final TypedAttribute<Instant> CREATED_AT =
            attr(TABLE, "createdAt", "created_at", Instant.class);

    public static final TypedAttribute<String> CREATED_BY =
            attr(TABLE, "createdBy", "created_by", String.class);

    public static final TypedAttribute<UUID> CREATED_BY_ID =
            attr(TABLE, "createdById", "created_by_id", UUID.class);

    public static final TypedAttribute<Instant> UPDATED_AT =
            attr(TABLE, "updatedAt", "updated_at", Instant.class);

    public static final TypedAttribute<String> UPDATED_BY =
            attr(TABLE, "updatedBy", "updated_by", String.class);

    public static final TypedAttribute<UUID> UPDATED_BY_ID =
            attr(TABLE, "updatedById", "updated_by_id", UUID.class);

    public static final TypedAttribute<Instant> DELETED_AT =
            attr(TABLE, "deletedAt", "deleted_at", Instant.class);

    public static final TypedAttribute<String> DELETED_BY =
            attr(TABLE, "deletedBy", "deleted_by", String.class);

    public static final TypedAttribute<UUID> DELETED_BY_ID =
            attr(TABLE, "deletedById", "deleted_by_id", UUID.class);
}

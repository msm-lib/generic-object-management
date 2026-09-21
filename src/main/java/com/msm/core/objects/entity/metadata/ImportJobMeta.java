package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.commons.GenericTypeResolverFactory.resolve;
import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class ImportJobMeta {

    private ImportJobMeta() {}

    public static final String OBJECT_NAME = "importjob";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("import_job"));

    // =========================================================
    // Primary
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    // =========================================================
    // Info
    // =========================================================

    public static final TypedAttribute<String> OBJECT_NAME_FIELD =
            attr(TABLE, "objectName", "object_name", String.class);

//    public static final TypedAttribute<UUID> ATTACHMENT_ID =
//            attr(TABLE, "attachmentId", "attachment_id", UUID.class);
//
//    public static final TypedAttribute<String> FILE_NAME =
//            attr(TABLE, "fileName", "file_name", String.class);
//
//    public static final TypedAttribute<String> FILE_PATH =
//            attr(TABLE, "filePath", "file_path", String.class);

    public static final TypedAttribute<String> STATUS =
            attr(TABLE, "status", "status", String.class);

    // =========================================================
    // Metrics
    // =========================================================

    public static final TypedAttribute<Long> TOTAL_ROWS =
            attr(TABLE, "totalRows", "total_rows", Long.class);

    public static final TypedAttribute<Long> VALID_ROWS =
            attr(TABLE, "validRows", "valid_rows", Long.class);

    public static final TypedAttribute<Long> SUCCESS_ROWS =
            attr(TABLE, "successRows", "success_rows", Long.class);

    public static final TypedAttribute<Long> ERROR_ROWS =
            attr(TABLE, "errorRows", "error_rows", Long.class);

    // =========================================================
    // Timing
    // =========================================================

    public static final TypedAttribute<Instant> STARTED_AT =
            attr(TABLE, "startedAt", "started_at", Instant.class);

    public static final TypedAttribute<Instant> COMPLETED_AT =
            attr(TABLE, "completedAt", "completed_at", Instant.class);

    // =========================================================
    // Error Info
    // =========================================================

    public static final TypedAttribute<String> ERROR_FILE_PATH =
            attr(TABLE, "errorFilePath", "error_file_path", String.class);

    public static final TypedAttribute<String> ERROR_MESSAGE =
            attr(TABLE, "errorMessage", "error_message", String.class);


    public static final TypedAttribute<Map<String, Object>> FILE_INFO =
            attr(TABLE, "fileInfo", "file_info", resolve(Map.class, String.class, Object.class));


    // =========================================================
    // Auditing (Inherited from AuditingEntity)
    // =========================================================

    public static final TypedAttribute<Instant> CREATED_AT =
            attr(TABLE, "createdAt", "created_at", Instant.class);

    public static final TypedAttribute<String> CREATED_BY =
            attr(TABLE, "createdBy", "created_by", String.class);

    public static final TypedAttribute<Instant> UPDATED_AT =
            attr(TABLE, "updatedAt", "updated_at", Instant.class);

    public static final TypedAttribute<String> UPDATED_BY =
            attr(TABLE, "updatedBy", "updated_by", String.class);
}

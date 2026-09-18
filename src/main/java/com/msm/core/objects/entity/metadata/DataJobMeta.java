package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.UUID;

import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class DataJobMeta {

    private DataJobMeta() {}

    public static final String OBJECT_NAME = "datajob";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("data_job"));

    // =========================================================
    // Primary
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    // =========================================================
    // Core Info
    // =========================================================

    public static final TypedAttribute<String> JOB_TYPE =
            attr(TABLE, "jobType", "job_type", String.class);

    public static final TypedAttribute<String> OBJECT_NAME_FIELD =
            attr(TABLE, "objectName", "object_name", String.class);

    public static final TypedAttribute<String> STATUS =
            attr(TABLE, "status", "status", String.class);

    public static final TypedAttribute<String> FILE_NAME =
            attr(TABLE, "fileName", "file_name", String.class);

    // =========================================================
    // Progress & Metrics
    // =========================================================

    public static final TypedAttribute<Integer> PROGRESS =
            attr(TABLE, "progress", "progress", Integer.class);

    // =========================================================
    // Timing
    // =========================================================

    public static final TypedAttribute<Instant> STARTED_AT =
            attr(TABLE, "startedAt", "started_at", Instant.class);

    public static final TypedAttribute<Instant> COMPLETED_AT =
            attr(TABLE, "completedAt", "completed_at", Instant.class);

    // =========================================================
    // Auditing
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


package com.msm.core.objects.dto.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.UUID;

public final class ObjectDependencyMeta {

    private ObjectDependencyMeta() {
    }

    public static final String OBJECT_NAME = "objectdependency";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("object_dependency"));

    // =========================
    // Base Fields
    // =========================

    public static final TypedAttribute<UUID> ID =
            new TypedAttribute<>(
                    "id",
                    UUID.class,
                    TABLE.field(DSL.name("id"), UUID.class)
            );

    public static final TypedAttribute<String> SOURCE_TYPE =
            new TypedAttribute<>(
                    "sourceType",
                    String.class,
                    TABLE.field(DSL.name("source_type"), String.class)
            );

    public static final TypedAttribute<UUID> SOURCE_ID =
            new TypedAttribute<>(
                    "sourceId",
                    UUID.class,
                    TABLE.field(DSL.name("source_id"), UUID.class)
            );

    public static final TypedAttribute<String> TARGET_TYPE =
            new TypedAttribute<>(
                    "targetType",
                    String.class,
                    TABLE.field(DSL.name("target_type"), String.class)
            );

    public static final TypedAttribute<UUID> TARGET_ID =
            new TypedAttribute<>(
                    "targetId",
                    UUID.class,
                    TABLE.field(DSL.name("target_id"), UUID.class)
            );

    public static final TypedAttribute<String> DEPENDENCY_FIELD =
            new TypedAttribute<>(
                    "dependencyField",
                    String.class,
                    TABLE.field(DSL.name("dependency_field"), String.class)
            );

    public static final TypedAttribute<String> DEPENDENCY_TYPE =
            new TypedAttribute<>(
                    "dependencyType",
                    String.class,
                    TABLE.field(DSL.name("dependency_type"), String.class)
            );

    public static final TypedAttribute<JSONB> CUSTOM_VALUES =
            new TypedAttribute<>(
                    "customValues",
                    JSONB.class,
                    TABLE.field(DSL.name("custom_values"), JSONB.class)
            );

    // =========================
    // Audit Fields
    // =========================

    public static final TypedAttribute<Instant> CREATED_AT =
            new TypedAttribute<>(
                    "createdAt",
                    Instant.class,
                    TABLE.field(DSL.name("created_at"), Instant.class)
            );

    public static final TypedAttribute<String> CREATED_BY =
            new TypedAttribute<>(
                    "createdBy",
                    String.class,
                    TABLE.field(DSL.name("created_by"), String.class)
            );

    public static final TypedAttribute<UUID> CREATED_BY_ID =
            new TypedAttribute<>(
                    "createdById",
                    UUID.class,
                    TABLE.field(DSL.name("created_by_id"), UUID.class)
            );

    public static final TypedAttribute<Instant> UPDATED_AT =
            new TypedAttribute<>(
                    "updatedAt",
                    Instant.class,
                    TABLE.field(DSL.name("updated_at"), Instant.class)
            );

    public static final TypedAttribute<String> UPDATED_BY =
            new TypedAttribute<>(
                    "updatedBy",
                    String.class,
                    TABLE.field(DSL.name("updated_by"), String.class)
            );

    public static final TypedAttribute<UUID> UPDATED_BY_ID =
            new TypedAttribute<>(
                    "updatedById",
                    UUID.class,
                    TABLE.field(DSL.name("updated_by_id"), UUID.class)
            );

    public static final TypedAttribute<Boolean> IS_DELETED =
            new TypedAttribute<>(
                    "isDeleted",
                    Boolean.class,
                    TABLE.field(DSL.name("is_deleted"), Boolean.class)
            );

    public static final TypedAttribute<Instant> DELETED_AT =
            new TypedAttribute<>(
                    "deletedAt",
                    Instant.class,
                    TABLE.field(DSL.name("deleted_at"), Instant.class)
            );

    public static final TypedAttribute<String> DELETED_BY =
            new TypedAttribute<>(
                    "deletedBy",
                    String.class,
                    TABLE.field(DSL.name("deleted_by"), String.class)
            );

    public static final TypedAttribute<UUID> DELETED_BY_ID =
            new TypedAttribute<>(
                    "deletedById",
                    UUID.class,
                    TABLE.field(DSL.name("deleted_by_id"), UUID.class)
            );
}
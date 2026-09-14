package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.commons.GenericTypeResolverFactory.resolve;
import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class ImportErrorMeta {

    private ImportErrorMeta() {}

    public static final String OBJECT_NAME = "importerror";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("bhc", "import_error"));

    // =========================================================
    // Primary & Foreign Keys
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    public static final TypedAttribute<UUID> IMPORT_ID =
            attr(TABLE, "importId", "import_id", UUID.class);

    // =========================================================
    // Location & Type
    // =========================================================

    public static final TypedAttribute<Long> ROW_NUMBER =
            attr(TABLE, "rowNumber", "row_number", Long.class);

    public static final TypedAttribute<String> ERROR_TYPE =
            attr(TABLE, "errorType", "error_type", String.class);

    public static final TypedAttribute<String> FIELD_NAME =
            attr(TABLE, "fieldName", "field_name", String.class);

    // =========================================================
    // Error Details
    // =========================================================

    public static final TypedAttribute<String> ERROR_CODE =
            attr(TABLE, "errorCode", "error_code", String.class);

    public static final TypedAttribute<String> ERROR_MESSAGE =
            attr(TABLE, "errorMessage", "error_message", String.class);


    public static final TypedAttribute<Map<String, Object>> DATA =
            attr(TABLE, "data", "data", resolve(Map.class, String.class, Object.class));

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

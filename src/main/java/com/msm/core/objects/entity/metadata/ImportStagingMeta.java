package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.commons.GenericTypeResolverFactory.resolve;
import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class ImportStagingMeta {

    private ImportStagingMeta() {}

    public static final String OBJECT_NAME = "importstaging";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("import_staging"));

    // =========================================================
    // Primary & Foreign Keys
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    public static final TypedAttribute<UUID> IMPORT_ID =
            attr(TABLE, "importId", "import_id", UUID.class);

    // =========================================================
    // Data
    // =========================================================

    public static final TypedAttribute<Long> ROW_NUMBER =
            attr(TABLE, "rowNumber", "row_number", Long.class);

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

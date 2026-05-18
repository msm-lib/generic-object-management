package com.msm.core.objects.dto.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.UUID;

public final class OutboxEntityMeta {

    private OutboxEntityMeta() {
    }

    public static final String OBJECT_NAME = "outbox";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("outbox"));

    // =========================
    // Base Fields
    // =========================

    public static final TypedAttribute<UUID> ID =
            new TypedAttribute<>(
                    "id",
                    UUID.class,
                    TABLE.field(DSL.name("id"), UUID.class)
            );

    /**
     * Object name
     */
    public static final TypedAttribute<String> AGGREGATE_TYPE =
            new TypedAttribute<>(
                    "aggregateType",
                    String.class,
                    TABLE.field(DSL.name("aggregate_type"), String.class)
            );

    /**
     * Same event type
     */
    public static final TypedAttribute<String> AGGREGATE_SUBTYPE =
            new TypedAttribute<>(
                    "aggregateSubtype",
                    String.class,
                    TABLE.field(DSL.name("aggregate_subtype"), String.class)
            );

    /**
     * Object id
     */
    public static final TypedAttribute<UUID> AGGREGATE_ID =
            new TypedAttribute<>(
                    "aggregateId",
                    UUID.class,
                    TABLE.field(DSL.name("aggregate_id"), UUID.class)
            );

    /**
     * Sent to
     */
    public static final TypedAttribute<String> DESTINATION =
            new TypedAttribute<>(
                    "destination",
                    String.class,
                    TABLE.field(DSL.name("destination"), String.class)
            );

    public static final TypedAttribute<JSONB> PAYLOAD =
            new TypedAttribute<>(
                    "payload",
                    JSONB.class,
                    TABLE.field(DSL.name("payload"), JSONB.class)
            );

    /**
     * PENDING, PROCESSING, COMPLETED, FAILED
     */
    public static final TypedAttribute<String> STATUS =
            new TypedAttribute<>(
                    "status",
                    String.class,
                    TABLE.field(DSL.name("status"), String.class)
            );

    public static final TypedAttribute<Integer> RETRY_COUNT =
            new TypedAttribute<>(
                    "retryCount",
                    Integer.class,
                    TABLE.field(DSL.name("retry_count"), Integer.class)
            );

    public static final TypedAttribute<String> RESPONSE_MESSAGE =
            new TypedAttribute<>(
                    "responseMessage",
                    String.class,
                    TABLE.field(DSL.name("response_message"), String.class)
            );

    public static final TypedAttribute<Integer> HTTP_STATUS =
            new TypedAttribute<>(
                    "httpStatus",
                    Integer.class,
                    TABLE.field(DSL.name("http_status"), Integer.class)
            );

    public static final TypedAttribute<Instant> CREATED_AT =
            new TypedAttribute<>(
                    "createdAt",
                    Instant.class,
                    TABLE.field(DSL.name("created_at"), Instant.class)
            );

    public static final TypedAttribute<Instant> UPDATED_AT =
            new TypedAttribute<>(
                    "updatedAt",
                    Instant.class,
                    TABLE.field(DSL.name("updated_at"), Instant.class)
            );
}
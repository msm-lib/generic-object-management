package com.msm.core.objects.entity.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.commons.GenericTypeResolverFactory.resolve;
import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class AttachmentMeta {

    private AttachmentMeta() {}

    public static final String OBJECT_NAME = "attachment";

    public static final Table<?> TABLE =
            DSL.table(DSL.name("attachment"));

    // =========================================================
    // Primary
    // =========================================================

    public static final TypedAttribute<UUID> ID =
            attr(TABLE, "id", "id", UUID.class);

    // =========================================================
    // File Info
    // =========================================================

    public static final TypedAttribute<String> FILE_NAME =
            attr(TABLE, "fileName", "file_name", String.class);

    public static final TypedAttribute<String> ORIGINAL_FILE_NAME =
            attr(TABLE, "originalFileName", "original_file_name", String.class);

    public static final TypedAttribute<String> FILE_URL =
            attr(TABLE, "fileUrl", "file_url", String.class);

    public static final TypedAttribute<String> FILE_TYPE =
            attr(TABLE, "fileType", "file_type", String.class);

    public static final TypedAttribute<String> DESCRIPTION =
            attr(TABLE, "description", "description", String.class);

    public static final TypedAttribute<String> MIME_TYPE =
            attr(TABLE, "mimeType", "mime_type", String.class);

    public static final TypedAttribute<String> FILE_EXTENSION =
            attr(TABLE, "fileExtension", "file_extension", String.class);

    // =========================================================
    // S3 / Storage Details
    // =========================================================

    public static final TypedAttribute<String> BUCKET_NAME =
            attr(TABLE, "bucketName", "bucket_name", String.class);

    public static final TypedAttribute<String> S3_KEY =
            attr(TABLE, "s3Key", "s3_key", String.class);

    public static final TypedAttribute<String> S3_VERSION_ID =
            attr(TABLE, "s3VersionId", "s3_version_id", String.class);

    public static final TypedAttribute<String> ETAG =
            attr(TABLE, "etag", "etag", String.class);

    public static final TypedAttribute<String> CHECKSUM_SHA256 =
            attr(TABLE, "checksumSha256", "checksum_sha256", String.class);

    public static final TypedAttribute<Long> CONTENT_LENGTH =
            attr(TABLE, "contentLength", "content_length", Long.class);

    public static final TypedAttribute<String> STORAGE_CLASS =
            attr(TABLE, "storageClass", "storage_class", String.class);

    public static final TypedAttribute<String> REGION =
            attr(TABLE, "region", "region", String.class);

    // =========================================================
    // Source Reference
    // =========================================================

    public static final TypedAttribute<String> SOURCE_TYPE =
            attr(TABLE, "sourceType", "source_type", String.class);

    public static final TypedAttribute<UUID> SOURCE_ID =
            attr(TABLE, "sourceId", "source_id", UUID.class);

    // =========================================================
    // Policy & Status
    // =========================================================

    public static final TypedAttribute<String> RETENTION_POLICY =
            attr(TABLE, "retentionPolicy", "retention_policy", String.class);

    public static final TypedAttribute<Instant> RETENTION_EXPIRES_AT =
            attr(TABLE, "retentionExpiresAt", "retention_expires_at", Instant.class);

    public static final TypedAttribute<String> STATUS =
            attr(TABLE, "status", "status", String.class);

    // =========================================================
    // System / Optimistic Locking
    // =========================================================

    public static final TypedAttribute<Long> VERSION =
            attr(TABLE, "version", "version", Long.class);

    public static final TypedAttribute<Map<String, Object>> CUSTOM_VALUES =
            attr(TABLE, "customValues", "custom_values", resolve(Map.class, String.class, Object.class));

    // =========================================================
    // Auditing / Lifecycle (Inherited / Common)
    // =========================================================

    public static final TypedAttribute<Instant> UPLOADED_AT =
            attr(TABLE, "uploadedAt", "uploaded_at", Instant.class);
}


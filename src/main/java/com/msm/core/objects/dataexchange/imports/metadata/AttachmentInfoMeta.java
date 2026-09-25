package com.msm.core.objects.dataexchange.imports.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;

import java.time.Instant;
import java.util.UUID;

import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public final class AttachmentInfoMeta {

    private AttachmentInfoMeta() {}


    public static final TypedAttribute<UUID> ID =
            attr("id", UUID.class);

    public static final TypedAttribute<String> ORIGINAL_FILE_NAME =
            attr("originalFileName", String.class);

    public static final TypedAttribute<String> FILE_TYPE =
            attr("fileType", String.class);

    public static final TypedAttribute<String> UPLOAD_URL =
            attr("uploadUrl", String.class);

    public static final TypedAttribute<String> S3_KEY =
            attr("s3Key", String.class);

    public static final TypedAttribute<String> DOWNLOAD_URL =
            attr("downloadUrl", String.class);

    public static final TypedAttribute<String> FILE_NAME =
            attr("fileName", String.class);

    public static final TypedAttribute<Instant> EXPIRES_AT =
            attr("expiresAt", Instant.class);

    public static final TypedAttribute<Long> EXPIRES_IN_MINUTES =
            attr("expiresInMinutes", Long.class);
}

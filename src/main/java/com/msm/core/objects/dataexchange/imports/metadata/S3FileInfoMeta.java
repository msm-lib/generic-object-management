package com.msm.core.objects.dataexchange.imports.metadata;

import com.msm.core.metadata.typesafe.TypedAttribute;

import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public class S3FileInfoMeta {
    public static final String OBJECT_NAME = "s3fileInfoMeta";


    public static final TypedAttribute<String> SERVICE =
            attr("service", String.class);

    public static final TypedAttribute<String> S3_KEY =
            attr("s3Key", String.class);

    public static final TypedAttribute<String> S3_FILE_NAME =
            attr("s3FileName", String.class);

    public static final TypedAttribute<String> ORIGINAL_FILE_NAME =
            attr("originalFileName", String.class);

    public static final TypedAttribute<String> FILE_TYPE =
            attr("fileType", String.class);

    public static final TypedAttribute<String> MIME_TYPE =
            attr("mimeType", String.class);

    public static final TypedAttribute<Long> CONTENT_LENGTH =
            attr("contentLength", Long.class);
}

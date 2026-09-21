package com.msm.core.objects.imports.dtometda;

import com.msm.core.metadata.typesafe.TypedAttribute;

import static com.msm.core.metadata.typesafe.MetaFieldBuilder.attr;

public class ImportJobDtoMeta {
    public static final String OBJECT_NAME = "importJobDtoMeta";

    public static final TypedAttribute<String> OBJECT_NAME_FIELD =
            attr("objectName", String.class);

    public static final TypedAttribute<String> SERVICE =
            attr("service", String.class);

    public static final TypedAttribute<String> FILE_NAME =
            attr("fileName", String.class);

    public static final TypedAttribute<String> ORIGINAL_FILE_NAME =
            attr("originalFileName", String.class);

    public static final TypedAttribute<String> MIME_TYPE =
            attr("mimeType", String.class);

    public static final TypedAttribute<Long> CONTENT_LENGTH =
            attr("contentLength", Long.class);


}

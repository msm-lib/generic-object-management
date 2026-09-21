package com.msm.core.objects.imports.s3;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.config.S3PropConfig;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.imports.dtometda.AttachmentInfoMeta;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.security.RequestContextHolder;
import com.msm.core.security.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class S3FileUtils {
    private static final String RETENTION_PERMANENT = "PERMANENT";
    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String DEFAULT_SCHEMA_FOLDER = "default";
    private static final String UPLOAD_METHOD = "PUT";
    private static final String DEFAULT_FILE_TYPE = "UNKNOWN";
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String DEFAULT_OBJECT_SEGMENT = "unknown";
    private static final int MAX_FILE_TYPE_LENGTH = 60;
    private static final DateTimeFormatter S3_DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final TypeReference<Map<String, Object>> PERSISTENCE_PAYLOAD_TYPE = new TypeReference<>() {
    };
    private static final String S3_SCHEME = "s3://";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3PropConfig s3PropConfig;



//    private String getOrDefaultOriginalFileName(Map<String, Object> uploadRequest) {
//        if (StringUtils.hasText(uploadRequest.getOriginalFileName())) {
//            return uploadRequest.getOriginalFileName();
//        }
//        return uploadRequest.getFileName();
//    }

    public String getBucketName() {
        return s3PropConfig.getBucketName();
    }


    public String s3FileUrl(String s3Key) {
        return S3_SCHEME + s3PropConfig.getBucketName() + "/" + s3Key;
    }

    public String getUploadUrl(String s3Key, String mimeType, Long contentLength) {
        PutObjectRequest.Builder putObjectRequest = PutObjectRequest
                .builder()
                .bucket(s3PropConfig.getBucketName())
                .key(s3Key)
                .contentType(Utils.STR.defaultIfBlank(mimeType, DEFAULT_MIME_TYPE));

        if (contentLength != null) {
            putObjectRequest.contentLength(contentLength);
        }

        Duration signatureDuration = Duration.ofMinutes(s3PropConfig.getPresignedUrlExpiration());
        Instant expiresAt = Instant.now().plus(signatureDuration);

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .putObjectRequest(putObjectRequest.build())
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toExternalForm();
    }

    private String createGetObjectUrl(String bucketName,
                                      String s3Key,
                                      String contentDisposition,
                                      String contentType,
                                      Duration signatureDuration) {
        GetObjectRequest.Builder getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .responseContentDisposition(contentDisposition);

        if (StringUtils.hasText(contentType)) {
            getObjectRequest.responseContentType(contentType);
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .getObjectRequest(getObjectRequest.build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
    }

    public Map<String, Object> getDownloadInfo(String objectName, DataRecord fileInfo) {
        Duration signatureDuration = Duration.ofMinutes(s3PropConfig.getPresignedUrlExpiration());
        Instant expiresAt = Instant.now().plus(signatureDuration);

        String contentDisposition = buildContentDisposition(
                "attachment",
                fileInfo.get(S3FileInfoMeta.ORIGINAL_FILE_NAME),
                fileInfo.get(S3FileInfoMeta.S3_FILE_NAME)
        );

        String downloadUrl = createGetObjectUrl(
                getBucketName(),
                getS3Key(objectName, fileInfo.get(S3FileInfoMeta.S3_FILE_NAME)),
                contentDisposition,
                null,
                signatureDuration
        );


        return DataRecord.of()
                .with(AttachmentInfoMeta.DOWNLOAD_URL, downloadUrl)
                .with(AttachmentInfoMeta.FILE_NAME, fileInfo.get(S3FileInfoMeta.ORIGINAL_FILE_NAME))
                .with(AttachmentInfoMeta.EXPIRES_AT, expiresAt)
                .with(AttachmentInfoMeta.EXPIRES_IN_MINUTES, s3PropConfig.getPresignedUrlExpiration())
                .getValues();
    }


    public Map<String, Object> getDownloadInfo(Map<String, Object> importJobMap) {
        DataRecord importJobRecord = DataRecord.of(importJobMap);
        DataRecord fileInfo = DataRecord.of(importJobRecord.get(ImportJobMeta.FILE_INFO));

        return getDownloadInfo(importJobRecord.get(ImportJobMeta.OBJECT_NAME_FIELD), fileInfo);
    }

    private String buildContentDisposition(String dispositionType, String originalFileName, String s3FileName) {
        String displayName = Utils.STR.defaultIfBlank(originalFileName, s3FileName);
        String fallbackFileName = sanitizeHeaderValue(displayName);
        String encodedS3FileName = encodeRfc5987FileName(s3FileName);

        return dispositionType + "; filename=\"" + fallbackFileName + "\"; filename*=UTF-8''" + encodedS3FileName;
    }

    private String sanitizeHeaderValue(String value) {
        return value.replace("\\", "")
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private String encodeRfc5987FileName(String value) {
        return URLEncoder.encode(sanitizeHeaderValue(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    //{tenantCode}/{subFolder}/{objectName}/{yyyyMMdd}/{fileName}
    // Exp: bhc/order/importjob/20260920/abcxyz.xlsx
    public String getS3Key(String objectName, String fileName) {
        String folder = buildFolderPath(objectName);
        return s3PropConfig.normalizeObjectKey(folder, fileName);
    }

    private String buildFolderPath(String objectName) {
        String schemaFolder = getOrDefaultRootFolder();
        return buildFolderPath(schemaFolder, objectName);
    }

    public String getOrDefaultRootFolder() {
        RequestContext ctx = RequestContextHolder.getRequestContext();
        if (Utils.STR.isNotBlank(ctx.getTenantCode())) {
            return ctx.getTenantCode();
        }

        return DEFAULT_SCHEMA_FOLDER;
    }


    public String getS3Key(String schemaFolder, String objectName, String fileName) {
        String folder = buildFolderPath(schemaFolder, objectName);
        return s3PropConfig.normalizeObjectKey(folder, fileName);
    }

    private String buildFolderPath(String schemaFolder, String objectName) {
        String subFolder = trimSlash(s3PropConfig.getSubFolder());
        String sourceFolder = Utils.STR.defaultIfBlank(objectName, DEFAULT_OBJECT_SEGMENT);
        String date = LocalDate.now().format(S3_DATE_PATH_FORMATTER);
        if (!StringUtils.hasText(subFolder)) {
            return schemaFolder + "/";
        }
        return String.format("%s/%s/%s/%s/", schemaFolder, subFolder, sourceFolder, date);
    }


    private String trimSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public String generateFileName(UUID objectId, String fileExtension) {
        UUID sourceId = Objects.isNull(objectId)
                ? UUID.randomUUID()
                : objectId;
        String generatedName = String.join("_", sourceId.toString(), String.valueOf(Instant.now().toEpochMilli()));
        if (StringUtils.hasText(fileExtension)) {
            return generatedName + "." + fileExtension;
        }
        return generatedName;
    }

    public String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }

        return null;
    }
}

package com.msm.core.objects.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aws.s3")
public class S3PropConfig {
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private boolean pathStyleAccessEnabled;
    private String region;
    private String subFolder;
    private long presignedUrlExpiration;

    public String normalizeObjectKey(String folder, String fileName) {
        String normalizedFolder = folder.endsWith("/") ? folder : folder + "/";
        String normalizedFileName = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        return normalizedFolder + normalizedFileName;
    }

    @Data
    public static class FolderConfig {
        private String folder;
    }
}

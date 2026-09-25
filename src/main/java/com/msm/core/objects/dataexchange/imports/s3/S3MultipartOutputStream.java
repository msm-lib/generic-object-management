package com.msm.core.objects.dataexchange.imports.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class S3MultipartOutputStream extends OutputStream {
    private final S3Client s3Client;
    private final String bucketName;
    private final String key;
    private final String uploadId;
    private final List<CompletedPart> completedParts;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private static final int FIVE_MB = 5 * 1024 * 1024; // Max = 5M S3 Part
    private int partNumber = 1;

    public S3MultipartOutputStream(S3Client s3Client, String bucketName, String key, String uploadId, List<CompletedPart> completedParts) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.key = key;
        this.uploadId = uploadId;
        this.completedParts = completedParts;
    }

    @Override
    public void write(int b) {
        buffer.write(b);
        if (buffer.size() >= FIVE_MB) {
            uploadPart();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        while (len > 0) {
            int remaining = FIVE_MB - buffer.size();
            int toWrite = Math.min(remaining, len);

            buffer.write(b, off, toWrite);

            off += toWrite;
            len -= toWrite;

            if (buffer.size() >= FIVE_MB) {
                uploadPart();
            }
        }
    }


    private void uploadPart() {
        if (buffer.size() == 0) return;

        byte[] partData = buffer.toByteArray();
        buffer.reset();

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartResponse response = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partData));

        completedParts.add(CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(response.eTag())
                .build());

        partNumber++;
    }

    @Override
    public void close() throws IOException {
        uploadPart();
        buffer.close();
    }
}

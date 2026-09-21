package com.msm.core.objects.imports.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class S3MultipartOutputStream0 extends OutputStream {

    private static final int PART_SIZE = 10 * 1024 * 1024; // 10 MB
    private final S3Client s3Client;
    private final String bucket;
    private final String key;
    private final String uploadId;
    private final List<CompletedPart> completedParts;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(PART_SIZE);
    private int partNumber = 1;
    private boolean closed = false;

    public S3MultipartOutputStream0(
            S3Client s3Client,
            String bucket,
            String key,
            String uploadId,
            List<CompletedPart> completedParts
    ) {

        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.uploadId = uploadId;
        this.completedParts = completedParts;
    }


    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        buffer.write(b);
        if (buffer.size() >= PART_SIZE) {
            uploadPart();
        }
    }


    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {

        ensureOpen();

        while (length > 0) {

            int remaining = PART_SIZE - buffer.size();
            int writeLength = Math.min(remaining, length);
            buffer.write(
                    bytes,
                    offset,
                    writeLength
            );
            offset += writeLength;
            length -= writeLength;
            if (buffer.size() >= PART_SIZE) {
                uploadPart();
            }
        }
    }


    private void uploadPart() {

        if (buffer.size() == 0) {
            return;
        }

        byte[] data = buffer.toByteArray();

        UploadPartRequest uploadRequest = UploadPartRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength((long) data.length)
                .build();

        UploadPartResponse response = s3Client.uploadPart(uploadRequest, RequestBody.fromBytes(data));

        CompletedPart completedPart = CompletedPart
                .builder()
                .partNumber(partNumber)
                .eTag(response.eTag())
                .build();

        completedParts.add(completedPart);
        partNumber++;
        buffer.reset();
    }

    @Override
    public void flush() throws IOException {
        ensureOpen();
    }

    @Override
    public void close() throws IOException {

        if (closed) {
            return;
        }

        try {
            if (buffer.size() > 0) {
                uploadPart();
            }
        } finally {
            closed = true;
            buffer.close();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("S3MultipartOutputStream is already closed");
        }
    }
}
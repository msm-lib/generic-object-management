package com.msm.core.objects.imports.s3;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExcelOriginalMultipartAsyncService {
    private final GenericObjectInternalService genericObjectInternalService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final S3Client s3Client;
    private final S3FileUtils s3FileUtils;

//    @Async
    @Transactional(readOnly = true)
    public void writeErrorsToOriginalSheetMultipartAsync(DataRecord importJobRecord) {
        UUID importHistoryId = importJobRecord.get(ImportJobMeta.ID);
        log.info("Start generate errors file: {}", importHistoryId);
        DataRecord fileInfoRecord = DataRecord.ofNullable(importJobRecord.get(ImportJobMeta.FILE_INFO));

        String fileKey = fileInfoRecord.get(S3FileInfoMeta.S3_KEY);
        String contentType = fileInfoRecord.get(S3FileInfoMeta.MIME_TYPE);
        String bucketName = s3FileUtils.getBucketName();


        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest
                .builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        List<CompletedPart> completedParts = new ArrayList<>();

        List<Map<String, Object>> errors = internalObjectQueryRepository.findByCondition(
                ImportErrorMeta.OBJECT_NAME,
                ImportErrorMeta.IMPORT_ID.getField().eq(importHistoryId)
        );

        Map<Long, DataRecord> dataError = Utils.D.groupBy(
                errors,
                objectMap -> (Long) objectMap.get(ImportErrorMeta.ROW_NUMBER.getFieldName()),
                DataRecord::ofNullable
        );
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(fileKey).build();

        try (
                ResponseInputStream<GetObjectResponse> s3InputStream = s3Client.getObject(getObjectRequest);
                XSSFWorkbook workbook = new XSSFWorkbook(s3InputStream);
                // Using SXSSFWorkbook to Streaming write (Hold 100 row on ram)
                SXSSFWorkbook targetWorkbook = new SXSSFWorkbook(workbook, 100, true);
                S3MultipartOutputStream0 s3Out = new S3MultipartOutputStream0(s3Client, bucketName, fileKey, uploadId, completedParts)
        ) {

            Sheet originalSheet = targetWorkbook.getXSSFWorkbook().getSheetAt(0);

            int errorColumnIndex = -1;
            Row headerRow = originalSheet.getRow(0);
            if (headerRow != null) {
                int totalCols = headerRow.getLastCellNum();
                for (int i = 0; i < totalCols; i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null && "Result".equalsIgnoreCase(cell.getStringCellValue())) {
                        errorColumnIndex = i;
                        break;
                    }
                }
                if (errorColumnIndex == -1) {
                    errorColumnIndex = totalCols;
                }
            }

            if (errorColumnIndex != -1) {
                int lastRowIndex = originalSheet.getLastRowNum();

                for (int excelRowIndex = 1; excelRowIndex <= lastRowIndex; excelRowIndex++) {
                    Row row = originalSheet.getRow(excelRowIndex);
                    if (row == null) continue;

                    String errorMessage = "";
                    DataRecord dataRecord = dataError.get((long)excelRowIndex);

                    if (dataRecord != null) {
                        errorMessage = dataRecord.get(ImportErrorMeta.ERROR_MESSAGE);
                    }

                    Cell errorCell = getOrCreate(row, errorColumnIndex);
                    errorCell.setCellValue(errorMessage);
                }
            }

            // ==========================================
            // Write excel to multipart
            // ==========================================
            targetWorkbook.write(s3Out);
            targetWorkbook.close();
            s3Out.close();
            //complete write multipart
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName).key(fileKey).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build()).build());

        } catch (Exception e) {
            log.error("Error while process multipart upload to s3: {}", e.getMessage(), e);
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName).key(fileKey).uploadId(uploadId).build());
        }
    }

    public Cell getOrCreate(Row row, int errorColumnIndex) {
        Cell errorCell = row.getCell(errorColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        if(errorCell == null){
            errorCell = row.createCell(errorColumnIndex, CellType.STRING);
        }

        return errorCell;
    }








//    public void exportToS3(
//            String bucketName,
//            String targetKey
//    ) {
//
//        String uploadId = null;
//        SXSSFWorkbook workbook = null;
//
//        List<CompletedPart> completedParts = new ArrayList<>();
//
//        try {
//
//            // =========================================================
//            // 1. Create S3 Multipart Upload
//            // =========================================================
//
//            CreateMultipartUploadRequest createRequest =
//                    CreateMultipartUploadRequest.builder()
//                            .bucket(bucketName)
//                            .key(targetKey)
//                            .contentType(
//                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
//                            )
//                            .build();
//
//            CreateMultipartUploadResponse createResponse =
//                    s3Client.createMultipartUpload(createRequest);
//
//            uploadId = createResponse.uploadId();
//
//            // =========================================================
//            // 2. Create SXSSFWorkbook
//            //
//            // Chỉ giữ tối đa 100 rows trong memory.
//            // Các rows cũ sẽ được flush xuống temporary files.
//            // =========================================================
//
//            workbook = new SXSSFWorkbook(100);
//
//            // Optional:
//            // Giảm khả năng temp file quá lớn bằng cách nén XML.
//            workbook.setCompressTempFiles(true);
//
//            Sheet sheet = workbook.createSheet("Result");
//
//            // =========================================================
//            // 3. Header
//            // =========================================================
//
//            Row headerRow = sheet.createRow(0);
//
//            headerRow.createCell(0)
//                    .setCellValue("Mã");
//
//            headerRow.createCell(1)
//                    .setCellValue("Tên");
//
//            headerRow.createCell(2)
//                    .setCellValue("Errors");
//
//            // =========================================================
//            // 4. Create S3 Multipart Output Stream
//            // =========================================================
//
//            try (
//                    S3MultipartOutputStream s3Out =
//                            new S3MultipartOutputStream(
//                                    s3Client,
//                                    bucketName,
//                                    targetKey,
//                                    uploadId,
//                                    completedParts
//                            )
//            ) {
//
//                // =====================================================
//                // 5. DB Cursor
//                //
//                // QUAN TRỌNG:
//                // databaseService.streamData() phải thực sự dùng
//                // DB cursor / fetchSize.
//                //
//                // KHÔNG được:
//                //
//                // List<Entity> data = repository.findAll();
//                //
//                // =====================================================
//
//                AtomicInteger rowNumber = new AtomicInteger(1);
//
//                databaseService.streamData(dbRecord -> {
//
//                    // =================================================
//                    // 6. Validate / Transform
//                    // =================================================
//
//                    ValidatedRow result =
//                            validateAndTransform(dbRecord);
//
//                    // =================================================
//                    // 7. Create Excel row
//                    // =================================================
//
//                    Row excelRow =
//                            sheet.createRow(rowNumber.getAndIncrement());
//
//                    // Mã
//                    excelRow.createCell(0)
//                            .setCellValue(
//                                    safeString(result.getCode())
//                            );
//
//                    // Tên
//                    excelRow.createCell(1)
//                            .setCellValue(
//                                    safeString(result.getName())
//                            );
//
//                    // Errors
//                    excelRow.createCell(2).setCellValue(safeString(result.getErrors()));
//
//                    // =================================================
//                    // Không được giữ dbRecord/result ở đâu cả.
//                    // Sau callback, object sẽ có thể được GC.
//                    // =================================================
//                });
//
//                // =====================================================
//                // 8. Write XLSX → S3
//                //
//                // SXSSFWorkbook sẽ đọc temporary files và ghi
//                // workbook xuống S3MultipartOutputStream.
//                // =====================================================
//
//                workbook.write(s3Out);
//
//                s3Out.flush();
//            }
//
//            // =========================================================
//            // 9. Complete S3 Multipart Upload
//            // =========================================================
//
//            CompleteMultipartUploadRequest completeRequest =
//                    CompleteMultipartUploadRequest.builder()
//                            .bucket(bucketName)
//                            .key(targetKey)
//                            .uploadId(uploadId)
//                            .multipartUpload(
//                                    CompletedMultipartUpload.builder()
//                                            .parts(completedParts)
//                                            .build()
//                            )
//                            .build();
//
//            s3Client.completeMultipartUpload(completeRequest);
//
//            // Upload đã complete.
//            // Không abort nữa.
//            uploadId = null;
//
//        } catch (Exception e) {
//
//            // =========================================================
//            // 10. Abort Multipart Upload nếu có lỗi
//            // =========================================================
//
//            if (uploadId != null) {
//
//                try {
//
//                    s3Client.abortMultipartUpload(
//                            AbortMultipartUploadRequest.builder()
//                                    .bucket(bucketName)
//                                    .key(targetKey)
//                                    .uploadId(uploadId)
//                                    .build()
//                    );
//
//                } catch (Exception abortException) {
//
//                    e.addSuppressed(abortException);
//                }
//            }
//
//            throw new RuntimeException(
//                    "Lỗi export database -> Excel -> S3",
//                    e
//            );
//
//        } finally {
//
//            // =========================================================
//            // 11. Cleanup SXSSF temporary files
//            //
//            // CỰC KỲ QUAN TRỌNG
//            // =========================================================
//
//            if (workbook != null) {
//
//                try {
//                    workbook.dispose();
//                } catch (Exception cleanupException) {
//
//                    cleanupException.printStackTrace();
//                }
//            }
//        }
//    }
//
//
//    /**
//     * Convert null -> empty string.
//     */
//    private String safeString(String value) {
//        return value != null ? value : "";
//    }
}


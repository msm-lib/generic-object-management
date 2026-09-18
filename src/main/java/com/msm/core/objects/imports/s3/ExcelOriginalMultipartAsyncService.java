package com.msm.core.objects.imports.s3;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.entity.metadata.AttachmentMeta;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
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


//    @Async
    @Transactional(readOnly = true)
    public void writeErrorsToOriginalSheetMultipartAsync(DataRecord importJobRecord) {
        UUID importHistoryId = importJobRecord.get(ImportJobMeta.ID);
        log.info("Start generate errors file: {}", importHistoryId);
        DataRecord attachmentInfoRecord = DataRecord.of(genericObjectInternalService.getObjectById(
                AttachmentMeta.OBJECT_NAME,
                importJobRecord.get(ImportJobMeta.ATTACHMENT_ID)
        ));

        String fileKey = attachmentInfoRecord.get(AttachmentMeta.S3_KEY);
        String contentType = attachmentInfoRecord.get(AttachmentMeta.MIME_TYPE);
        String bucketName = attachmentInfoRecord.get(AttachmentMeta.BUCKET_NAME);


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
                S3MultipartOutputStream s3Out = new S3MultipartOutputStream(s3Client, bucketName, fileKey, uploadId, completedParts)
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
}


package com.msm.core.objects.imports.excel;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.entity.metadata.ExportJobMeta;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.objects.imports.model.ExportStatus;
import com.msm.core.objects.imports.model.RawRow;
import com.msm.core.objects.imports.model.RowMappingContext;
import com.msm.core.objects.imports.s3.S3FileUtils;
import com.msm.core.objects.imports.s3.S3MultipartOutputStream0;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ExportExcelService {
    private final ExportExcelTemplateService exportExcelTemplateService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final S3Client s3Client;
    private final S3FileUtils s3FileUtils;
    private final ActionExecutor actionExecutor;
    private final ExportJobTransactionService exportJobTransactionService;
    private static final String TEMPLATE_KEY = "bhc/customer/profile/profile_template.xlsx";
    private static final String FILE_EXTENSION = "xlsx";
    private static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";


    @Async("hookTaskExecutor")
    public void exportExcelData(String schemaFolder, Map<String, Object> exportJobMap) {
        DataRecord exportJobRecord = DataRecord.ofNullable(exportJobMap);
        exportJobTransactionService.markExporting(exportJobRecord);

        Map<String, Object> filter = exportJobRecord.get(ExportJobMeta.FILTER_CRITERIA);
        ObjectFilterRequest request = Utils.O.toObject(filter, ObjectFilterRequest.class);
        request.setObjectInfo(ObjectFilterRequest.ObjectInfo.of(exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD)));


        byte[] templateBytes = exportExcelTemplateService.getTemplateBytes(s3FileUtils.getBucketName(), TEMPLATE_KEY);
        Map<Integer, String> attributeColumnMapping = exportExcelTemplateService.extractColumnHeaderMap(
                templateBytes,
                exportJobRecord.get(ExportJobMeta.ID),
                exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD)
        );

        UUID exportId = exportJobRecord.get(ExportJobMeta.ID);
        String objectName = exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD);
        String s3FileName = s3FileUtils.generateFileName(exportId, FILE_EXTENSION);





        String s3Key = s3FileUtils.getS3Key(schemaFolder, objectName, s3FileName);
        String contentType = MIME_TYPE;
        String bucketName = s3FileUtils.getBucketName();




        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest
                .builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        List<CompletedPart> completedParts = new ArrayList<>();
        AtomicLong totalRow = new AtomicLong(0);
        try (
                ByteArrayInputStream s3InputStream = new ByteArrayInputStream(templateBytes);
                XSSFWorkbook workbook = new XSSFWorkbook(s3InputStream);
                // Using SXSSFWorkbook to Streaming write (Hold 100 row on ram)
                SXSSFWorkbook targetWorkbook = new SXSSFWorkbook(workbook, 100, true);
                S3MultipartOutputStream0 s3Out = new S3MultipartOutputStream0(s3Client, bucketName, s3Key, uploadId, completedParts)
        ) {

            Sheet originalSheet = targetWorkbook.getXSSFWorkbook().getSheetAt(0);

            int startRowIndex = originalSheet.getLastRowNum() + 1;
            AtomicInteger rowIndex = new AtomicInteger(startRowIndex);

            int batchSize = 100;
            internalObjectQueryRepository.filterStream(
                    objectName,
                    request,
                    batchSize,
                    dataMap -> {

                        Row row = originalSheet.createRow(rowIndex.getAndIncrement());
                        Map<String, Object> rowMappingData = rowMapping(exportId, row.getRowNum(),objectName, attributeColumnMapping, dataMap);

                        totalRow.incrementAndGet();
                        attributeColumnMapping.forEach((columnIndex, fieldName) -> {
                            Object value = rowMappingData.get(fieldName);
                            row.createCell(columnIndex).setCellValue(value != null ? value.toString() : "");
                        });
                    }
            );

            targetWorkbook.write(s3Out);
            targetWorkbook.close();
            s3Out.close();
            //complete write multipart
            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(CompleteMultipartUploadRequest
                    .builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build());
            log.info(
                    "S3 OBJECT EXISTS. bucket={}, key={}, eTag={}",
                    bucketName,
                    s3Key,
                    response.eTag()
            );

            DataRecord s3FileInfoRecord = DataRecord
                    .of()
                    .with(S3FileInfoMeta.S3_KEY, s3Key)
                    .with(S3FileInfoMeta.S3_FILE_NAME, s3FileName)
                    .with(S3FileInfoMeta.ORIGINAL_FILE_NAME, s3FileName)
                    .with(S3FileInfoMeta.FILE_TYPE, FILE_EXTENSION)
                    .with(S3FileInfoMeta.MIME_TYPE, MIME_TYPE);
            exportJobRecord
                    .with(ExportJobMeta.TOTAL_ROWS, totalRow.get())
                    .with(ExportJobMeta.COMPLETED_AT, Instant.now())
                    .with(ExportJobMeta.FILE_INFO, s3FileInfoRecord.getValues())
                    .with(ExportJobMeta.STATUS, ExportStatus.COMPLETED.name());
            exportJobTransactionService.markCompleted(exportJobRecord);
        } catch (Exception e) {
            log.error("Error while process multipart upload to s3: {}", e.getMessage(), e);
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName).key(s3Key).uploadId(uploadId).build());
            exportJobRecord
                    .with(ExportJobMeta.TOTAL_ROWS, totalRow.get())
                    .with(ExportJobMeta.COMPLETED_AT, Instant.now())
                    .with(ExportJobMeta.ERROR_MESSAGE, e.getMessage())
                    .with(ExportJobMeta.STATUS, ExportStatus.FAILED.name());
            exportJobTransactionService.markFailed(exportJobRecord);
            throw Lombok.sneakyThrow(e);
        }
    }


    private Map<String, Object> rowMapping(
            UUID exportId,
            long rowNumber,
            String objectName,
            Map<Integer, String> headerColumn,
            Map<String, Object> row
    ) {

        RowMappingContext<Map<String, Object>> mapperContext = RowMappingContext.of(
                exportId,
                rowNumber,
                objectName,
                headerColumn,
                row
        );
        ActionContext<RowMappingContext<Map<String, Object>>> actionContext = ActionContext
                .<RowMappingContext<Map<String, Object>>>builder()
                .resource(objectName)
                .action(ObjectActionNamed.Excel.Export.ROW_MAPPING)
                .payload(mapperContext)
                .build();

        return actionExecutor.execute(actionContext);
    }
}

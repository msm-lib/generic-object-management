package com.msm.core.objects.imports.excel;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.filter.domain.pageable.PageRequest;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectExportRegistry;
import com.msm.core.objects.entity.metadata.ExportJobMeta;
import com.msm.core.objects.imports.ExportConfigService;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.objects.imports.model.ColumnHeaderDefinitionPath;
import com.msm.core.objects.imports.model.ExportCellMappingContext;
import com.msm.core.objects.imports.model.ExportRowMappingContext;
import com.msm.core.objects.imports.model.ExportStatus;
import com.msm.core.objects.imports.s3.S3FileUtils;
import com.msm.core.objects.imports.s3.S3MultipartOutputStream0;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.utils.JsonPathUtil;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ExportExcelService {
    private static final Set<String> IGNORE_ATTRIBUTE = Set.of("customValues");
    private final ExportExcelTemplateService exportExcelTemplateService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final S3Client s3Client;
    private final S3FileUtils s3FileUtils;
    private final ActionExecutor actionExecutor;
    private final ExportJobTransactionService exportJobTransactionService;
    private final ExportConfigService exportConfigService;
    private static final String FILE_EXTENSION = "xlsx";
    private static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";


    @Async("importExportDataTaskExecutor")
    public void exportExcelData(String schemaFolder, Map<String, Object> exportJobMap) {
        DataRecord exportJobRecord = DataRecord.ofNullable(exportJobMap);
        exportJobTransactionService.markExporting(exportJobRecord);
        log.info(
                "[EXPORT-ASYNC-1] ENTER exportExcelData, exportId={}, thread={}",
                exportJobMap.get(ExportJobMeta.ID),
                Thread.currentThread().getName()
        );

        String objectName = exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD);
        Map<String, Object> filter = exportJobRecord.get(ExportJobMeta.FILTER_CRITERIA);
        ObjectFilterRequest request = Utils.O.toObject(filter, ObjectFilterRequest.class);
        request.setObjectInfo(ObjectFilterRequest.ObjectInfo.of(objectName));

        String template = exportConfigService.getExportTemplate(objectName);

        byte[] templateBytes = exportExcelTemplateService.getTemplateBytes(s3FileUtils.getBucketName(), template);
        Map<Integer, ColumnHeaderDefinitionPath> attributeColumnMapping = exportExcelTemplateService.extractColumnHeaderMap(
                templateBytes,
                exportJobRecord.get(ExportJobMeta.ID),
                objectName
        );

        log.info("Detect data column header: {}", attributeColumnMapping);
        UUID exportId = exportJobRecord.get(ExportJobMeta.ID);
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

            Sheet originalSheet = targetWorkbook.getSheetAt(0);

            int startRowIndex = targetWorkbook.getXSSFWorkbook().getSheetAt(0).getLastRowNum() + 1;
            AtomicInteger rowIndex = new AtomicInteger(startRowIndex);

            ObjectExportRegistry.ProcessingConfig processingConfig = exportConfigService.getProcessingConfig(objectName);
            int batchSize = processingConfig.getBatchSize();

            PageResponse<Map<String, Object>> pageResponse = getData(objectName, request);

            while (Utils.CL.isNotEmpty(pageResponse.getContents())) {
                pageResponse.getContents().forEach(dataMap -> {
                    Map<String, Object> rowMappingData = rowMapping(exportId, rowIndex.get(), objectName, attributeColumnMapping, dataMap);
                    cellDataMapping(
                            exportId,
                            objectName,
                            attributeColumnMapping,
                            rowMappingData
                    );

                    //Ignore row data null
                    if(Objects.nonNull(rowMappingData)) {
                        Row row = originalSheet.createRow(rowIndex.getAndIncrement());
                        attributeColumnMapping.forEach((columnIndex, columnHeaderPath) -> {
                            Object value = rowMappingData.get(columnHeaderPath.originalPath());
                            if (Objects.isNull(value) && columnHeaderPath.isReference()) {
                                value = JsonPathUtil.extractValue(rowMappingData, columnHeaderPath.originalPath());
                            }
                            row.createCell(columnIndex).setCellValue(value != null ? value.toString() : "");
                        });

                        totalRow.incrementAndGet();
                    }
                });

                PageRequest pageRequest = request.getPageRequest();
                PageRequest newPageRequest = PageRequest.of(pageRequest.getPage() + 1, pageRequest.getSize(), pageRequest.getSorts());
                request.setPageRequest(newPageRequest);
                pageResponse = getData(objectName, request);

            }

//            internalObjectQueryRepository.filterStream(
//                    objectName,
//                    request,
//                    batchSize,
//                    dataMap -> {
//
//                        Map<String, Object> rowMappingData = rowMapping(exportId, rowIndex.get(), objectName, attributeColumnMapping, dataMap);
//                        cellDataMapping(
//                                exportId,
//                                objectName,
//                                attributeColumnMapping,
//                                rowMappingData
//                        );
//
//                        //Ignore row data null
//                        if(Objects.nonNull(rowMappingData)) {
//                            Row row = originalSheet.createRow(rowIndex.getAndIncrement());
//                            attributeColumnMapping.forEach((columnIndex, columnHeaderPath) -> {
//                                Object value = rowMappingData.get(columnHeaderPath.originalPath());
//                                if (Objects.isNull(value) && columnHeaderPath.isReference()) {
//                                    value = JsonPathUtil.extractValue(rowMappingData, columnHeaderPath.originalPath());
//                                }
//                                row.createCell(columnIndex).setCellValue(value != null ? value.toString() : "");
//                            });
//
//                            totalRow.incrementAndGet();
//                        }
//                    }
//            );
            log.info("End write data to excel file: {} row", totalRow.get());
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
                    .with(ExportJobMeta.FILE_NAME, s3FileName)
                    .with(ExportJobMeta.ORIGINAL_FILE_NAME, s3FileName)
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


    private PageResponse<Map<String, Object>> getData(String objectName, ObjectFilterRequest request) {
        ActionContext<ObjectFilterRequest> actionContext = ActionContext
                .<ObjectFilterRequest>builder()
                .resource(objectName)
                .action(Constants.FilterAction.FILTER_OBJECT)
                .payload(request)
                .build();
        return internalObjectQueryRepository.filter(actionContext);
    }


    private Map<String, Object> rowMapping(
            UUID exportId,
            long rowNumber,
            String objectName,
            Map<Integer, ColumnHeaderDefinitionPath> headerColumn,
            Map<String, Object> row
    ) {

        ExportRowMappingContext<Map<String, Object>> mapperContext = ExportRowMappingContext.of(
                exportId,
                rowNumber,
                objectName,
                headerColumn,
                row
        );
        ActionContext<ExportRowMappingContext<Map<String, Object>>> actionContext = ActionContext
                .<ExportRowMappingContext<Map<String, Object>>>builder()
                .resource(objectName)
                .action(ObjectActionNamed.Excel.Export.ROW_MAPPING)
                .payload(mapperContext)
                .build();

        return actionExecutor.execute(actionContext);
    }


    private void cellDataMapping(
            UUID jobId,
            String objectName,
            Map<Integer, ColumnHeaderDefinitionPath> headerColumn,
            Map<String, Object> rowData
    ) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attribute -> {
            if (rowData.containsKey(attribute.getFieldName())
                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
                String objectCellResource = Utils.STR.format("{0}.{1}",  objectName, attribute.getFieldName());
                ExportCellMappingContext cellMappingContext = ExportCellMappingContext.of(jobId, objectName, attribute, headerColumn, rowData);
                ActionContext<ExportCellMappingContext> actionContext = ActionContext
                        .<ExportCellMappingContext>builder()
                        .resource(objectCellResource)
                        .action(ObjectActionNamed.Excel.Export.CELL_MAPPING)
                        .payload(cellMappingContext)
                        .build();

                Object columnDataValue = actionExecutor.execute(actionContext);;
                rowData.put(attribute.getFieldName(), columnDataValue);
            }
        });
    }

}

//    private Cell customExcelCell(
//            UUID jobId,
//            String objectName,
//            Map<Integer, String> headerColumn,
//            Row row,
//            Object attributeValue
//    ) {

//        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
//        objectMetadata.getAttributes().forEach(attribute -> {
//            if (rowData.containsKey(attribute.getFieldName())
//                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
//                String objectCellResource = Utils.STR.format("{0}.{1}",  objectName, attribute.getFieldName());
//                CellMappingContext cellMappingContext = CellMappingContext.of(jobId, objectName, attribute, headerColumn, rowData);
//                ActionContext<CellMappingContext> actionContext = ActionContext
//                        .<CellMappingContext>builder()
//                        .resource(objectCellResource)
//                        .action(ObjectActionNamed.Excel.Export.CELL_MAPPING)
//                        .payload(cellMappingContext)
//                        .build();
//
//
//            }
//        });

//        actionExecutor.execute(actionContext);
//    }

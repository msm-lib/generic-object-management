package com.msm.core.objects.dataexchange.imports.excel;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.pageable.Sort;
import com.msm.core.filter.domain.pageable.SortDirection;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.ErrorHelper;
import com.msm.core.objects.dataexchange.imports.ErrorType;
import com.msm.core.objects.dataexchange.imports.ImportConfigService;
import com.msm.core.objects.dataexchange.imports.ImportErrorService;
import com.msm.core.objects.dataexchange.imports.ImportJobService;
import com.msm.core.objects.dataexchange.imports.ReferenceProcessService;
import com.msm.core.objects.dataexchange.imports.metadata.AttachmentInfoMeta;
import com.msm.core.objects.dataexchange.imports.metadata.S3FileInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.BatchInsertDataResult;
import com.msm.core.objects.dataexchange.imports.model.DownloadErrorContext;
import com.msm.core.objects.dataexchange.imports.model.ImportRow;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.ImportValidation;
import com.msm.core.objects.dataexchange.imports.model.ObjectImportContext;
import com.msm.core.objects.dataexchange.imports.model.RawRow;
import com.msm.core.objects.dataexchange.imports.model.ReadActionContext;
import com.msm.core.objects.dataexchange.imports.s3.ExcelOriginalMultipartAsyncService;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <pre>Import validate flow</pre>
 *<pre>
 * Import
 *    |
 * Import validate handler
 *    |
 * Read handler(CSV, EXCEL, ...)
 *    |
 * Detect header handler(by object)
 *    |
 * Row mapping handler(by object)
 *    |
 * Cell mapping handler(by object.fieldName)
 *    |
 * Reference resolve handler(object, List of row data)
 *    |
 * Batch validate handler(insert db (object, List row))
 *    |
 *  Result
 *</pre>
 * <pre>Import flow</pre>
 *<pre>
 * Import
 *    |
 * Import batch read from import stating(by config)
 *    |
 * Check import mode duplicate(upsert, lookup)
 *    |
 * Execute batch insert(by object)
 *    |
 * Tracking record inserted
 *    |
 *  Result
 *</pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ImportExcelService {

    private static final String IMPORT_JOB_ID_NAME = "importJobId";
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ActionExecutor actionExecutor;
    private final ReferenceProcessService referenceProcessService;
    private final ExcelOriginalMultipartAsyncService excelOriginalMultipartAsyncService;
    private final S3FileUtils s3FileUtils;
    private final ImportConfigService importConfigService;
    private final ImportDataExecutor importDataExecutor;
    private final ImportJobService importJobService;
    private final ImportErrorService importErrorService;


    @Handler(action = ObjectActionNamed.Excel.IMPORT_DATA)
    public Map<String, Object> importData(ActionContext<ObjectImportContext> actionContext) {
        ObjectImportContext objectImportContext = actionContext.getPayload();
        try {
            importJobService.makeJobImporting(objectImportContext.jobId());
            long lastRowNumber = 0;

            int batchSize = importConfigService.getProcessingConfig(objectImportContext.importObjectName()).getBatchSize();

            while (true) {

                List<Map<String, Object>> rows = internalObjectQueryRepository.findByCondition(
                        ImportStagingMeta.OBJECT_NAME,
                        ImportStagingMeta.IMPORT_ID.getField().eq(objectImportContext.jobId())
                                .and(ImportStagingMeta.ROW_NUMBER.getField().gt(
                                        lastRowNumber
                                )),
                        batchSize,
                        List.of(Sort.of(ImportStagingMeta.ROW_NUMBER.getFieldName(), SortDirection.ASC)),
                        List.of(
                                ImportStagingMeta.ROW_NUMBER.getFieldName(),
                                ImportStagingMeta.IDENTITY_KEY.getFieldName(),
                                ImportStagingMeta.IDENTITY_LEVEL.getFieldName(),
                                ImportStagingMeta.DATA.getFieldName()
                        )
                );

                if (rows.isEmpty()) {
                    break;
                }

                BatchInsertDataResult result = importDataExecutor.batchInsertDataAction(
                        objectImportContext.jobId(),
                        objectImportContext.importObjectName(),
                        rows
                );

                internalObjectQueryRepository.updateWithExpressions(
                        ImportJobMeta.OBJECT_NAME,
                        ImportJobMeta.ID.getField().eq(objectImportContext.jobId()),
                        Map.of(
                                ImportJobMeta.SUCCESS_ROWS.getFieldName(),
                                ImportJobMeta.SUCCESS_ROWS.getField().add(result.successCount()),
                                ImportJobMeta.ERROR_ROWS.getFieldName(),
                                ImportJobMeta.ERROR_ROWS.getField().add(result.failedCount())
                        )
                );


                lastRowNumber = DataRecord.of(rows.getLast()).get(ImportStagingMeta.ROW_NUMBER);
            }

            return finishImport(objectImportContext.jobId());
        } catch (Exception e) {
            log.error("Import failed", e);
            throw Lombok.sneakyThrow(e);
        } finally {
            finishImport(objectImportContext.jobId());
        }
    }

    private Map<String, Object> finishImport(UUID importId) {

        // success/error count
        // COMPLETED hoặc COMPLETED_WITH_ERRORS
        internalObjectQueryRepository.updateWithExpressions(
                ImportJobMeta.OBJECT_NAME,
                ImportJobMeta.ID.getField().eq(importId),
                Map.of(
                        ImportJobMeta.STATUS.getFieldName(),
                        DSL.choose()
                                .when(ImportJobMeta.ERROR_ROWS.getField().gt(0L), ImportStatus.COMPLETED_WITH_ERRORS.name())
                                .otherwise(ImportStatus.COMPLETED.name()),
                        ImportJobMeta.COMPLETED_AT.getFieldName(), Instant.now()
                )
        );


        return internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importId);

    }


    @Handler(action = ObjectActionNamed.Excel.VALIDATION)
    public Map<String, Object> validate(ActionContext<Map<String, Object>> actionContext) {
        UUID importId = DataRecord.of(actionContext.getPayload()).get(IMPORT_JOB_ID_NAME, UUID.class);
        List<DataRecord> errors = new ArrayList<>();
        try {
            DataRecord importJobRecord = DataRecord.of(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importId));
            DataRecord attachmentRecord = DataRecord.of(s3FileUtils.getDownloadInfo(importJobRecord.asMap()));
            log.info("Processing file: {}", attachmentRecord.get(AttachmentInfoMeta.FILE_NAME));
            String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
            ImportValidation importValidation = ImportValidation.of(importId, actionContext.getResource(), fileUrl);
            importJobService.makeJobValidating(importId);

            final Map<Integer, String>[] cachedHeaderMap = new Map[]{null};

            ObjectImportRegistry.HeaderConfig header = importConfigService.getHeader(actionContext.getResource());
            int batchSize = importConfigService.getProcessingConfig(actionContext.getResource()).getBatchSize();
            List<ImportRow> batchBuffer = new ArrayList<>();
            Consumer<RawRow<Row>> rowConsumer = rowRawRow -> {
                if(header.isAutoDetectHeader()) {
                    if (cachedHeaderMap[0] == null) {
                        Map<Integer, String> detectedColumnHeaderMap = importDataExecutor.detectColumnHeaderMapping(importId, rowRawRow);
                        if (Utils.CL.isEmpty(detectedColumnHeaderMap)) {
                            return;
                        }
                        cachedHeaderMap[0] = detectedColumnHeaderMap;
                        return;
                    }
                } else {
                    if (cachedHeaderMap[0] == null) {
                        if(rowRawRow.rowNumber() < header.getRow()) {
                            return;
                        }
                        Map<Integer, String> detectedColumnHeaderMap = importDataExecutor.detectColumnHeaderMapping(importId, rowRawRow);
                        if (Utils.CL.isEmpty(detectedColumnHeaderMap)) {
                            throw new IllegalArgumentException("Not found column header mapping to object attribute at row: " + header.getRow());
                        }
                        cachedHeaderMap[0] = detectedColumnHeaderMap;
                        return;
                    }
                }


                Map<String, Object> rowData = null;
                try {
                    rowData = importDataExecutor.rowMapping(importId, cachedHeaderMap[0], rowRawRow);
                } catch (Exception e) {
                    errors.add(DataRecord
                            .of()
                            .with(ImportErrorMeta.IMPORT_ID, importId)
                            .with(ImportErrorMeta.ROW_NUMBER, rowRawRow.rowNumber())
                            .with(ImportErrorMeta.ERROR_TYPE, ErrorType.VALIDATION_ERROR)
                            .with(ImportErrorMeta.FIELD_NAME, null)
                            .with(ImportErrorMeta.ERROR_MESSAGE, ErrorHelper.resolveErrorMessage(e))
                            .with(ImportErrorMeta.ERROR_CODE, ErrorType.ROW_MAPPING_ERROR)
                            .with(ImportErrorMeta.DATA, null)
                    );
                }

                if(Utils.CL.isNotEmpty(rowData)) {
                    try {
                        importDataExecutor.cellMapping(importId, rowRawRow.objectName(), cachedHeaderMap[0], rowData);
                    } catch (Exception e) {
                        errors.add(DataRecord
                                .of()
                                .with(ImportErrorMeta.IMPORT_ID, importId)
                                .with(ImportErrorMeta.ROW_NUMBER, rowRawRow.rowNumber())
                                .with(ImportErrorMeta.ERROR_TYPE, ErrorType.VALIDATION_ERROR)
                                .with(ImportErrorMeta.FIELD_NAME, null)
                                .with(ImportErrorMeta.ERROR_MESSAGE, ErrorHelper.resolveErrorMessage(e))
                                .with(ImportErrorMeta.ERROR_CODE, ErrorType.CELL_MAPPING_ERROR)
                                .with(ImportErrorMeta.DATA, rowData)
                        );
                    }
                    batchBuffer.add(ImportRow.of(rowRawRow.rowNumber(), rowData));
                    if (batchBuffer.size() >= batchSize) {
                        executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
                        batchBuffer.clear();
                    }
                }
            };

            ReadActionContext<Row> request = ReadActionContext.of(importValidation.importObjectName(), importId, importValidation.fileUrl(), rowConsumer);

            ActionContext<ReadActionContext<Row>> actionRequest = ActionContext
                    .<ReadActionContext<Row>>builder()
                    .resource(importValidation.importObjectName())
                    .action(ObjectActionNamed.Excel.READ_FILE)
                    .payload(request)
                    .build();

            actionExecutor.execute(actionRequest);

            if (!batchBuffer.isEmpty()) {
                executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
            }

            if(!errors.isEmpty()) {
                importErrorService.insertErrorsIgnoreDuplicate(errors);
            }

            return finishValidation(importValidation.importId());
        } catch (Exception e) {
            importJobService.makeJobValidateFailed(importId);
            if(!errors.isEmpty()) {
                importErrorService.insertErrorsIgnoreDuplicate(errors);
            }
            log.error("Exception occurred when validating the object", e);
            throw Lombok.sneakyThrow(e);
        }
    }


    private void executeBatchProcessing(UUID importId, String importObjectName, List<ImportRow> rows) {
        trackingRowProcessing(importId, rows.size());
        referenceProcessService.batchRefProcessing(importId, importObjectName, rows);
        importDataExecutor.batchDataValidateAction(importId, importObjectName, rows);
    }

    private Map<String, Object> finishValidation(UUID importId) {

        // query lại DB
        // nếu errorRows = 0 => VALIDATED
        // ngược lại => VALIDATION_FAILED

        //Check duplicate
        //SELECT
        //    data ->> 'code' AS code,
        //    COUNT(*)
        //FROM bhc.import_staging
        //WHERE import_id = :importId
        //GROUP BY data ->> 'code'
        //HAVING COUNT(*) > 1;

        //SELECT s.row_number
        //FROM bhc.import_staging s
        //JOIN bhc.profile p
        //    ON p.code = s.data ->> 'code'
        //WHERE s.import_id = :importId
        //  AND (
        //       p.is_deleted = false
        //       OR p.is_deleted IS NULL
        //  );

        // implementation phía dưới

        internalObjectQueryRepository.updateWithExpressions(
                ImportJobMeta.OBJECT_NAME,
                ImportJobMeta.ID.getField().eq(importId),
                Map.of(
                        ImportJobMeta.STATUS.getFieldName(),
                        DSL.choose()
                                .when(ImportJobMeta.ERROR_ROWS.getField().gt(0L), ImportStatus.VALIDATION_FAILED.name())
                                .otherwise(ImportStatus.VALIDATED.name())
                )
        );


        return internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importId);
    }

    private void trackingRowProcessing(UUID importId, long rowCount) {
        internalObjectQueryRepository.updateWithExpressions(
                ImportJobMeta.OBJECT_NAME,
                ImportJobMeta.ID.getField().eq(importId),
                Map.of(ImportJobMeta.TOTAL_ROWS.getFieldName(), ImportJobMeta.TOTAL_ROWS.getField().add(rowCount))
        );
    }


    @Handler(action = ObjectActionNamed.Excel.DOWNLOAD_FILE_ERRORS)
    public Map<String, Object> downloadErrors(ActionContext<DownloadErrorContext> actionContext) {
        DataRecord dataRecord = DataRecord.ofNullable(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, actionContext.getPayload().importId()));

        String s3KeyErrorFile = dataRecord.get(ImportJobMeta.ERROR_FILE_PATH);
        if(s3KeyErrorFile != null) {
            return s3FileUtils.getDownloadInfo(dataRecord.asMap());
        }
        excelOriginalMultipartAsyncService.writeErrorsToOriginalSheetMultipartAsync(dataRecord);
        DataRecord fileInfo = DataRecord.ofNullable(dataRecord.get(ImportJobMeta.FILE_INFO));
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                actionContext.getPayload().importId(),
                Map.of(ImportJobMeta.ERROR_FILE_PATH.getFieldName(), fileInfo.get(S3FileInfoMeta.S3_KEY))
        );
        return s3FileUtils.getDownloadInfo(dataRecord.asMap());
    }
}


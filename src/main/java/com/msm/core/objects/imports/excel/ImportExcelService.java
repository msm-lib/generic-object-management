package com.msm.core.objects.imports.excel;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.pageable.Sort;
import com.msm.core.filter.domain.pageable.SortDirection;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dto.QueryTemplate;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.imports.BatchImportService;
import com.msm.core.objects.imports.ImportConfigService;
import com.msm.core.objects.imports.ImportJobService;
import com.msm.core.objects.imports.ImportTransactionExecutor;
import com.msm.core.objects.imports.ReferenceProcessService;
import com.msm.core.objects.imports.dtometda.AttachmentInfoMeta;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.objects.imports.model.BatchInsertDataResult;
import com.msm.core.objects.imports.model.DownloadErrorContext;
import com.msm.core.objects.imports.model.ImportRow;
import com.msm.core.objects.imports.model.ImportStatus;
import com.msm.core.objects.imports.model.ImportValidation;
import com.msm.core.objects.imports.model.ObjectImportContext;
import com.msm.core.objects.imports.model.RawRow;
import com.msm.core.objects.imports.model.ReadActionContext;
import com.msm.core.objects.imports.s3.ExcelOriginalMultipartAsyncService;
import com.msm.core.objects.imports.s3.S3FileUtils;
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
    private final BatchImportService batchImportService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ActionExecutor actionExecutor;
    private final ReferenceProcessService referenceProcessService;
    private final ExcelOriginalMultipartAsyncService excelOriginalMultipartAsyncService;
    private final S3FileUtils s3FileUtils;
    private final ImportConfigService importConfigService;
    private final ImportDataExecutor importDataExecutor;
    private final ImportJobService importJobService;
    private final ImportTransactionExecutor importTransactionExecutor;


    @Handler(action = ObjectActionNamed.Excel.IMPORT_FILE)
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
        try {
            DataRecord importJobRecord = DataRecord.of(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importId));
            DataRecord attachmentRecord = DataRecord.of(s3FileUtils.getDownloadInfo(importJobRecord.asMap()));

            log.warn("Processing file: {}", attachmentRecord.get(AttachmentInfoMeta.FILE_NAME));
            String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
//        String fileUrl = "https://msm-digiretail-dev-s3-data-001.s3.ap-southeast-1.amazonaws.com/bhc/masterData/v2order/xlsx/2026/09/16/v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789542224338_72b918a5.xlsx?response-content-disposition=attachment%3B%20filename%3D%22v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789542224338_72b918a5.xlsx%22%3B%20filename%2A%3DUTF-8%27%27KHGH_SharePoint_Template_Draft.xlsx&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEEwaDmFwLXNvdXRoZWFzdC0xIkgwRgIhAP%2FACqHTGLLN6fUzjjN9YNPkXCzbFenVxtRD3Ua6hBcyAiEAoO9SS%2B%2BYQM3GwhEibovEWdXVhfW3xPNwNU9O2buMdU0qmAQIFRAAGgwwNzE0MTgwMTkwNzAiDD3Vi1QiYKpaBqhYyCr1AwEPY7DAssM53K7hQROs8%2FdVP42YRW0vbGfmt2UPZk2C3b%2BrgB56c8ymzkWlAALvNiT%2Fvk%2BtM4ycDdhgPp7%2BbTH8n4XGOndtxWukYHrVrJDHUjxWCO%2B8Q6I89LFilLvbtRTodaDaDarLIsELa0UA%2BIvxaVFaybwReddLBWAkspIzdI%2BuYztTJIVlXgpAqZulOOhqB69%2FCXDQGYaFEEtblp2R45eMEMKagy9rUrqnTyFkzcUDwSHfDTzEbzQ24h69mHQdxdSGgrcmqwwlNr4k8PHY%2FFhlvt5lH02aqhUYD5eJ73iTtK37Kc44GG7HRXGgvNN8EuSKJWLiQikQNs7cBcVg2f0hn4w5x568WfBtC8%2FEeX4nTxJj8uiLLKeY1ETQ4XUj7Qz9tWkoR43OMT9XU51e1eqoK3LO6ay5W8PZRQMTE5ePPTUESAj%2FjMV6ZA%2FkfuAWoBvxjfWuLAwpfabc6igU%2FAwCXUn7GfQ4glLrSktSBtvYT8ccEOdTN0oahAWMRajY9QT%2F2W5%2BCXI%2FTLe7mWe2hILFWV084itDizn8ohdZk0CbtMO95VLteeApvuNo%2B27bS5TmZIBkJNVFsAUprnc2adQaMABKdsQzUrUeWOvX%2BqIW5FQlTrNCSkj6HVJ1utgbZoZpfl2NKxEKwo%2Fx7j%2B1Q9uzbjDzgarVBjqlAXeAPaAA4Ca%2BMDY1sNWk51T8Wj%2BFoeeYxHWOjhZuHpcv9KjjR6ltIgoXihMniNF%2Bj366LiW4R5EDLAnV7ip8VEgON32zyWjOkiFcpckDk2A6psjO0suNsoWWOePkSjzYIgvFczMxK5yszFLm8yHEu38iLoMa7NSasT6z12z5pb%2FUCsh5S%2F%2FUGLo4EubYeJUd5D7Krxi2otEDtEGoHn0TkwJeiLB8mw%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260916T144424Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIARBIGYPT7EDID2OFG%2F20260916%2Fap-southeast-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=8ee70a363bb1e645b4c78fae688edba4156cbaf64e1a3b4e4fd4f9f5755abcfc";
            ImportValidation importValidation = ImportValidation.of(importId, actionContext.getResource(), fileUrl);

//            internalObjectQueryRepository.update(
//                    ImportJobMeta.OBJECT_NAME,
//                    importId,
//                    DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATING.name()).getValues()
//            );

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


                Map<String, Object> rowData = importDataExecutor.rowMapping(importId, cachedHeaderMap[0], rowRawRow);
                if(Utils.CL.isNotEmpty(rowData)) {
                    importDataExecutor.cellMapping(importId, rowRawRow.objectName(), cachedHeaderMap[0], rowData);
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

            return finishValidation(importValidation.importId());
        } catch (Exception e) {
            log.error("Exception occurred when validating the object", e);
            importJobService.makeJobValidateFailed(importId);
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



    public static QueryTemplate createQueryAttachmentInfo(Object attachmentId) {
        Map<String, Object> parameters = Utils.CL.newHashMap("id", attachmentId);
        return QueryTemplate.builder()
                .query("attachment-download-info")
                .parameters(parameters)
                .build();
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


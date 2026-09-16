package com.msm.core.objects.imports;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.filter.domain.pageable.Sort;
import com.msm.core.filter.domain.pageable.SortDirection;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.dto.QueryTemplate;
import com.msm.core.objects.entity.metadata.AttachmentInfoMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.imports.model.BatchImportResult;
import com.msm.core.objects.imports.model.BatchRowData;
import com.msm.core.objects.imports.model.CellMapperContext;
import com.msm.core.objects.imports.model.ImportRow;
import com.msm.core.objects.imports.model.ImportStatus;
import com.msm.core.objects.imports.model.ImportValidation;
import com.msm.core.objects.imports.model.ObjectImportContext;
import com.msm.core.objects.imports.model.RawRow;
import com.msm.core.objects.imports.model.ReadActionContext;
import com.msm.core.objects.imports.model.RowMapperContext;
import com.msm.core.objects.imports.service.ReferenceProcessService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ImportService {
    private static final Set<String> IGNORE_ATTRIBUTE = Set.of("customValues");
    private static final String ATTACHMENT_OBJECT_NAME = "attachment";
    private static final String IMPORT_JOB_ID_NAME = "importJobId";
    private final BatchImportService batchImportService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ActionExecutor actionExecutor;
    private final GenericObjectConfigProperties config;
    private final GenericObjectInternalService genericObjectInternalService;
    private final ReferenceProcessService referenceProcessService;


    @Handler(action = ObjectActionNamed.Csv.IMPORT_FILE)
    public Map<String, Object> importData(ActionContext<ObjectImportContext> actionContext) {

        ObjectImportContext objectImportContext = actionContext.getPayload();
        long lastRowNumber = 0;
        int batchSize = config.getImportFile().batchSize(actionContext.getResource());

        while (true) {

            List<Map<String, Object>> rows = internalObjectQueryRepository.findByCondition(
                    ImportStagingMeta.OBJECT_NAME,
                    ImportStagingMeta.IMPORT_ID.getField().eq(objectImportContext.importJob())
                            .and(ImportStagingMeta.ROW_NUMBER.getField().gt(
                                    lastRowNumber
                            )),
                    batchSize,
                    List.of(Sort.of(ImportStagingMeta.ROW_NUMBER.getFieldName(), SortDirection.ASC)),
                    List.of(ImportStagingMeta.ROW_NUMBER.getFieldName(), ImportStagingMeta.DATA.getFieldName())
            );

            if (rows.isEmpty()) {
                break;
            }

            BatchImportResult result = batchImportService.importBatch(
                    objectImportContext.importJob(),
                    objectImportContext.importObjectName(),
                    rows
            );

            internalObjectQueryRepository.updateWithExpressions(
                    ImportJobMeta.OBJECT_NAME,
                    ImportJobMeta.ID.getField().eq(objectImportContext.importJob()),
                    Map.of(
                            ImportJobMeta.SUCCESS_ROWS.getFieldName(),
                            ImportJobMeta.SUCCESS_ROWS.getField().add(result.successCount()),
                            ImportJobMeta.ERROR_ROWS.getFieldName(),
                            ImportJobMeta.ERROR_ROWS.getField().add(result.failedCount())
                    )
            );


            lastRowNumber = DataRecord.of(rows.getLast()).get(ImportStagingMeta.ROW_NUMBER);
        }

        return finish(objectImportContext.importJob());
    }

    private Map<String, Object> finish(UUID importId) {

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


    @Handler(action = ObjectActionNamed.Csv.VALIDATION)
    public Map<String, Object> validate(ActionContext<Map<String, Object>> actionContext) {
        UUID importId = DataRecord.of(actionContext.getPayload()).get(IMPORT_JOB_ID_NAME, UUID.class);
        //importJobId
        DataRecord importJobRecord = DataRecord.of(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importId));

        Map<String, Object> attachmentDownloadInfo = genericObjectInternalService.query(
                ATTACHMENT_OBJECT_NAME,
                createQueryAttachmentInfo(importJobRecord.get(ImportJobMeta.ATTACHMENT_ID))
        );

        DataRecord attachmentRecord = DataRecord.of(attachmentDownloadInfo);

        log.warn("Processing file: {}", attachmentRecord.get(AttachmentInfoMeta.FILE_NAME));
        String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
//        String fileUrl = "https://msm-digiretail-dev-s3-data-001.s3.ap-southeast-1.amazonaws.com/bhc/masterData/v2order/csv/2026/09/16/v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789540156927_1974e902.csv?response-content-disposition=attachment%3B%20filename%3D%22v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789540156927_1974e902.csv%22%3B%20filename%2A%3DUTF-8%27%27Profile_validate_success.csv&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEEYaDmFwLXNvdXRoZWFzdC0xIkYwRAIgNk4CJHL08GJG7onhWTZE08VNmAzWmp89srAOp9GbqO4CIGqDqf7CSLb3ZGpOPWxRMW0ScXgbqN7qB9AHy%2FOiw3rnKpgECA8QABoMMDcxNDE4MDE5MDcwIgxO81kEgP%2F%2FKP0%2BiDoq9QMbRmp6Qw3cEjypRcZUjkI3KJz%2BIE4bc%2FxQEqzWZdGBtCQrDBO63cWIvmvwx%2B4Pe%2Bf1H1ng1LYiis2wqm%2FSRHRpK8%2BWnvP1UTnGUl5B5qfy%2FoY1ddsIwT%2FuGtQHjiWJRVghowKdQ4AHLEG2yijIu5GOQgdxPs3y4NKheC3rSxkXQD%2F8hnfdtsTD1v1dfbxo%2FoNAgU6upkLhFJFFgxhPbRquAnFfBdHImfsOW9CsDy48hGaHl51PN%2B%2BqL6hjvp6cCnSo3suEEzMidWYgK0ETQ2eNr9guAQPc1x8YNhu0l8AsP0g0V3FKTHckcT33vRXc5bnnWYK0NVAC8dK%2BZs8mGnJJwQayO2y0mycnX8KU2ziE%2BM%2F1TB3dp8W6YzqZV1kKmB8R%2B0uB1ZKyVrhzbP1ESVTcsMbJaLgSPbKHNJL8W5PlAUfoBnGjUhX2NVfgcKUI5PHcwVrX0NB1mRUp4MyhCLrWweHw2vFUI8n496jscagaIkqbv%2Fm6L56gxT%2FZjucMiQqtXk2oQFZb0B0Gt%2Fjrpew%2BwB3fKWvWwZB99O3dGiUHoPckh2saDte0Efo96%2BEBbAN%2BoIAa9ymwBARiOy3%2BbtC9i6YHsxa5iisWDDRI0iTri5TO%2FH4L9HRt0inemIFvVNAVpm9Xf6TL8IGV8BcjcylsbKzxQcUwuOSo1QY6pwHlvjL5ZuP3ISZdTQGIie8GGBzjF8qpXQfRgEOaNZngISXhIKI7H7IIbQU3D8FDX%2FzUG3qa6eS3zn9R7pzOous1ekyWcHsRQJiYXLiAFhkXCU5fEHpWBk3lugUkjBcJR9h8DXcnliCwNh4fDOHQsec1jsbMgRwq6ip7PcfmNkCpJEwst3JrQqPM7g9ZLss%2Bx8aa70cyqVPqnX24UyeFrMzpr3ZB%2FeJ4rw%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260916T062925Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIARBIGYPT7ID2NHERR%2F20260916%2Fap-southeast-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=458f4cc613bb6aa487ec8d3d01705146ab8cdb423988a7ec25715cc62151c2ad";
        ImportValidation importValidation = ImportValidation.of(importId, actionContext.getResource(), fileUrl);

        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                importId,
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATING.name()).getValues()
        );

//        final Map<Integer, String>[] cachedHeaderMap = new Map[]{null};
//        GenericObjectConfigProperties.Header header = config.getImportFile().header(actionContext.getResource());

        int batchSize = config.getImportFile().batchSize(actionContext.getResource());
        List<ImportRow> batchBuffer = new ArrayList<>();
        Consumer<RawRow<CSVRecord>> rowConsumer = importRow -> {
            Map<String, Object> rowData = rowMapping(importId, importRow);
            if(Utils.CL.isNotEmpty(rowData)) {
                cellMapping(importId, importRow.objectName(), rowData);
                batchBuffer.add(ImportRow.of(importRow.rowNumber(), rowData));
                if (batchBuffer.size() >= batchSize) {
                    executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
                    batchBuffer.clear();
                }
            }
        };

        ReadActionContext<CSVRecord> request = ReadActionContext.of(importValidation.importObjectName(), importValidation.fileUrl(), rowConsumer);

        ActionContext<ReadActionContext<CSVRecord>> actionRequest = ActionContext
                .<ReadActionContext<CSVRecord>>builder()
                .resource(importValidation.importObjectName())
                .action(ObjectActionNamed.Csv.READ_FILE)
                .payload(request)
                .build();

        actionExecutor.execute(actionRequest);

        if (!batchBuffer.isEmpty()) {
            executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
        }

        return finishValidation(importValidation.importId());
    }


    private Map<String, Object> rowMapping(UUID importId, RawRow<CSVRecord> row) {

        RowMapperContext<CSVRecord> mapperContext = RowMapperContext.of(
                importId,
                row.rowNumber(),
                row.objectName(),
                row.data()
        );
        ActionContext<RowMapperContext<CSVRecord>> actionContext = ActionContext
                .<RowMapperContext<CSVRecord>>builder()
                .resource(row.objectName())
                .action(ObjectActionNamed.Csv.ROW_MAPPING)
                .payload(mapperContext)
                .build();

        return actionExecutor.execute(actionContext);
    }


    private void cellMapping(UUID importId, String objectName, Map<String, Object> rowData) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attribute -> {
            if (rowData.containsKey(attribute.getFieldName())
                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
                String objectCellResource = Utils.STR.format("{0}.{1}",  objectName, attribute.getFieldName());
                CellMapperContext cellMapperContext = CellMapperContext.of(importId, objectName, attribute, rowData);
                ActionContext<CellMapperContext> actionContext = ActionContext
                        .<CellMapperContext>builder()
                        .resource(objectCellResource)
                        .action(ObjectActionNamed.Csv.CELL_MAPPING)
                        .payload(cellMapperContext)
                        .build();

                Object columnDataValue = actionExecutor.execute(actionContext);;
                rowData.put(attribute.getFieldName(), columnDataValue);
            }
        });
    }


    private void executeBatchProcessing(UUID importId, String importObjectName, List<ImportRow> rows) {
        trackingRowProcessing(importId, rows.size());
        referenceProcessService.batchRefProcessing(importId, importObjectName, rows);
        batchDataProcessAction(importId, importObjectName, rows);
    }

    private void batchDataProcessAction(UUID importId, String importObjectName, List<ImportRow> rows) {
        ActionContext<BatchRowData> actionContext = ActionContext
                .<BatchRowData>builder()
                .resource(importObjectName)
                .action(ObjectActionNamed.Csv.BATCH_ROW_DATA_PROCESSING)
                .payload(BatchRowData.of(importId, importObjectName, rows))
                .build();
        actionExecutor.execute(actionContext);
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

}


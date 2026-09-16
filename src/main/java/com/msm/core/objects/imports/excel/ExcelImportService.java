package com.msm.core.objects.imports.excel;

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
import com.msm.core.objects.imports.BatchImportService;
import com.msm.core.objects.imports.model.BatchImportResult;
import com.msm.core.objects.imports.model.BatchRowData;
import com.msm.core.objects.imports.model.CellMapperContext;
import com.msm.core.objects.imports.model.ColumnHeaderMapperContext;
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
import org.apache.poi.ss.usermodel.Row;
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
public class ExcelImportService {
    private static final Set<String> IGNORE_ATTRIBUTE = Set.of("customValues");
    private static final String ATTACHMENT_OBJECT_NAME = "attachment";
    private static final String IMPORT_JOB_ID_NAME = "importJobId";
    private final BatchImportService batchImportService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ActionExecutor actionExecutor;
    private final GenericObjectConfigProperties config;
    private final GenericObjectInternalService genericObjectInternalService;
    private final ReferenceProcessService referenceProcessService;


    @Handler(action = ObjectActionNamed.Excel.IMPORT_FILE)
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


    @Handler(action = ObjectActionNamed.Excel.VALIDATION)
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
//        String fileUrl = "https://msm-digiretail-dev-s3-data-001.s3.ap-southeast-1.amazonaws.com/bhc/masterData/v2order/xlsx/2026/09/15/v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789453625040_75b6a0f6.xlsx?response-content-disposition=attachment%3B%20filename%3D%22v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789453625040_75b6a0f6.xlsx%22%3B%20filename%2A%3DUTF-8%27%27KHGH_SharePoint_Template_Draft.xlsx&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEDQaDmFwLXNvdXRoZWFzdC0xIkcwRQIhANoYTarqZa51zU3Lz4GN3KxWclueLG2Qxt9Ai2GMZgNWAiAaRtVHFYcodRmwrzEvlEZcbC9dY%2FLjqBrm%2FsA0wVLUUiqhBAj9%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDA3MTQxODAxOTA3MCIM8ORG%2FtdaWGtIL%2Fn6KvUDOlywcaKRkUoJIXF2v017Nh9Zq%2BLaqZxpr%2BZ1breANrt0oSGNyR7wGMSqzgnBF6gBlq88js9UCav5rmL7hU9lmFFlIFLZWM5kawfu8T7og46LO1aHXdHEp%2F62h7OioAJnSLbiDAoAori0VL%2Fg2u3tGzxzh95nF46wZh5V%2FYrBXPpt2H%2FgwzDD9KNXbXv9UxGZp0H6g6GuaTl10voMwfrtnwu5KDxVagta9%2BMVEcDy3tL0vyKtLnBVGSN%2Bkl%2FWrM5q%2B10wcSNOADGkBP%2BMYSSYNX567HM6y7b2fjhSHstsmT%2BwW79GNP5RErFI8YjJ9vzGUfoC0kbiSKX9Bg844u3W7jWtJ%2FY%2B%2BRpLq3QcKt7jQ0egO%2F%2BJBsqSJ7nkyHhyem8EtQ1FMADq0XYElcQP3qk5oysS%2FCcTQMy%2BPpc2Qm%2FZPR%2B9%2FGTLJ0IQPIubxr6evBeht3riccYs8QURw5CYaNwi2JJuppgxwcRMSX%2F0vIeZZ6Bxpj317Se6EOdAx0QRvs9F9vMF%2Bx6k64deMKictHobgJSTObOORUw3l3MB775d2wCh2jkxFfBBRkJK6GsT%2Buk50xbTuCCqp5RMl4SLgEyT5UrR3hDP8FXkXUPuZelHA3M%2FYDCNDzAQMDnISsbXfZ4u%2BJtBAEo3SfZuHMX66LrIasFy3BQZMM%2FbpNUGOqYBtKG%2B%2BKk10bY0hjwQWYDqLP2DMmsHPbVLbtbiY4IyYWrvENDdho5olNfaFZTNcBLiMa6W5WMqjj9xfu240FjRA8sBhEDf78dGw3HGBaYNpVpv6Hh8A3S3Ep0x3J4fNctSeH9rJvHmolTOMu04mz2VfIKAPrsy5woXffCM3jroQ428yv0yTp5bctWkCTdfeGivw3EYthaIMWzyaBmalVBkB90dbpGx%2FA%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260915T132355Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIARBIGYPT7HLNLLCB6%2F20260915%2Fap-southeast-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=86ff15bee5708a397a64981f886f022b499dd43cda8560c95b24274492f973d9";
        ImportValidation importValidation = ImportValidation.of(importId, actionContext.getResource(), fileUrl);

        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                importId,
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATING.name()).getValues()
        );

        final Map<Integer, String>[] cachedHeaderMap = new Map[]{null};

        GenericObjectConfigProperties.Header header = config.getImportFile().header(actionContext.getResource());
        int batchSize = config.getImportFile().batchSize(actionContext.getResource());
        List<ImportRow> batchBuffer = new ArrayList<>();
        Consumer<RawRow<Row>> rowConsumer = rowRawRow -> {
            if(header.isAutoDetectHeader()) {
                if (cachedHeaderMap[0] == null) {
                    Map<Integer, String> detectedColumnHeaderMap = detectColumnHeaderMapping(importId, rowRawRow);
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
                    Map<Integer, String> detectedColumnHeaderMap = detectColumnHeaderMapping(importId, rowRawRow);
                    if (Utils.CL.isEmpty(detectedColumnHeaderMap)) {
                        throw new IllegalArgumentException("Not found column header mapping to object attribute at row: " + header.getRow());
                    }
                    cachedHeaderMap[0] = detectedColumnHeaderMap;
                    return;
                }
            }


            Map<String, Object> rowData = rowMapping(importId, cachedHeaderMap[0], rowRawRow);
            if(Utils.CL.isNotEmpty(rowData)) {
                cellMapping(importId, rowRawRow.objectName(), cachedHeaderMap[0], rowData);
                batchBuffer.add(ImportRow.of(rowRawRow.rowNumber(), rowData));
                if (batchBuffer.size() >= batchSize) {
//                    batchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
                    executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
                    batchBuffer.clear();
                }
            }
        };

        ReadActionContext<Row> request = ReadActionContext.of(importValidation.importObjectName(), importValidation.fileUrl(), rowConsumer);

        ActionContext<ReadActionContext<Row>> actionRequest = ActionContext
                .<ReadActionContext<Row>>builder()
                .resource(importValidation.importObjectName())
                .action(ObjectActionNamed.Excel.READ_FILE)
                .payload(request)
                .build();

        actionExecutor.execute(actionRequest);

        if (!batchBuffer.isEmpty()) {
//            batchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
            executeBatchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
        }

        return finishValidation(importValidation.importId());
    }

    private Map<Integer, String> detectColumnHeaderMapping(UUID importId, RawRow<Row> row) {

        ColumnHeaderMapperContext<Row> mapperContext = ColumnHeaderMapperContext.of(
                importId,
                row.rowNumber(),
                row.objectName(),
                row.data()
        );
        ActionContext<ColumnHeaderMapperContext<Row>> actionContext = ActionContext
                .<ColumnHeaderMapperContext<Row>>builder()
                .resource(row.objectName())
                .action(ObjectActionNamed.Excel.DETECT_COLUMN_HEADER_MAPPING)
                .payload(mapperContext)
                .build();

        Map<Integer, String> columnHeaderMap = actionExecutor.execute(actionContext);
        if(Utils.CL.isEmpty(columnHeaderMap)) {
            return null;
        }
        return columnHeaderMap;
    }

    private Map<String, Object> rowMapping(UUID importId, Map<Integer, String> headerColumn, RawRow<Row> row) {

        RowMapperContext<Row> mapperContext = RowMapperContext.of(
                importId,
                row.rowNumber(),
                row.objectName(),
                headerColumn,
                row.data()
        );
        ActionContext<RowMapperContext<Row>> actionContext = ActionContext
                .<RowMapperContext<Row>>builder()
                .resource(row.objectName())
                .action(ObjectActionNamed.Excel.ROW_MAPPING)
                .payload(mapperContext)
                .build();

        return actionExecutor.execute(actionContext);
    }


    private void cellMapping(UUID importId, String objectName, Map<Integer, String> columnHeader, Map<String, Object> rowData) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attribute -> {
            if (rowData.containsKey(attribute.getFieldName())
                    && !IGNORE_ATTRIBUTE.contains(attribute.getFieldName())) {
                String objectCellResource = Utils.STR.format("{0}.{1}",  objectName, attribute.getFieldName());
                CellMapperContext cellMapperContext = CellMapperContext.of(importId, objectName, attribute, columnHeader, rowData);
                ActionContext<CellMapperContext> actionContext = ActionContext
                        .<CellMapperContext>builder()
                        .resource(objectCellResource)
                        .action(ObjectActionNamed.Excel.CELL_MAPPING)
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
                .action(ObjectActionNamed.Excel.BATCH_ROW_DATA_PROCESSING)
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


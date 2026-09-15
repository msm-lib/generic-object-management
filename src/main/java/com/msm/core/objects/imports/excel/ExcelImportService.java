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
//        String fileUrl = "https://msm-digiretail-dev-s3-data-001.s3.ap-southeast-1.amazonaws.com/bhc/masterData/v2order/xlsx/2026/09/14/v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789381617383_69de0605.xlsx?response-content-disposition=attachment%3B%20filename%3D%22v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789381617383_69de0605.xlsx%22%3B%20filename%2A%3DUTF-8%27%27KHGH_SharePoint_Template_Draft.xlsx&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEB0aDmFwLXNvdXRoZWFzdC0xIkgwRgIhAKS5hwIP00R0g%2FDK3gXNmL2DIApBjwHRYRE9nn2SGfdoAiEAmYWP5VT0lTqDI1AC%2BYAept5FnUuEFF%2FMFBoM6mIZguUqoQQI5v%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgwwNzE0MTgwMTkwNzAiDH%2Fo8KFU7OsnZG4tUyr1A0AHPL8R%2Fr7g8rpQxeGoKNcXGZ7W3g6hyBvngcqZfp4SdGnTGo7H3nQkErqsPaXTN4WQaGjvIn6%2Bev6vKXxljOK4mx6nzX%2BsrWbL6iR9mM9aUCnYq8e4ORjYGEob6pNHe%2BOYsrjQgRHQe4SlLBGlAEFME4%2BKfQGvD0s6Y%2BXJVGUnBcdlCjgZvRHiAqQ%2Bknw2jaui0CpKF5j0x6ml%2BXfTjRvwUATh%2FuigaoqcU01w1hE63kopSbbvDcfX65yaTJP5jLbQAJwqpJwORjtL61%2BWGsk0d8r2Z%2FkNO2UdvTy2iViOolksLuubCQxnzkcb3ci8Sjm8FpUiwkOdOW%2B2gpCweKjaUW1n2aCbiK8YS%2Fh57VTGSFc1vg0O4Hos5hMRSooWuquzsCqXtlKOJ4BF3Y7VHDaw3EvpxtcwIZN4M95Ws7REBXXr8G%2BtbJRpmMKmILu9i653girYpXRIE8mTfTFQPZP1NmfftUTs8%2FjGbPSdXnFnGvEoAwFEC9Ts2%2BLsbYpBx%2FQHoL%2FybmE1x59LstN5JEQwR%2BuUUvwDkRQZ9iev3GsM8MnIDEsK%2BFhaSQhQ7Ur2w0tPUU11cRFE5rm8m%2F3LmRTZcQ2aBLQuH8qIp2UjYL9bI2mDhLc%2FVKl2zPDp4rnDpkKGjUfUMYMhfzsw%2BP%2BK2TRNzN%2FnKTCw35%2FVBjqlAVvtaZCmLxZeWwStTCNZxKvmjEkjUIjMCj2mPEbZBDTtcUICueBX13RSZuMwc18zldMg80tlENNjvrPeisnoWkZwYP12t1yVXOLuHz0c1b%2FtU%2FdUSCs%2FA8z7RYZVCCerRzIGDPR3aDUNjbTUKA%2B4xsL6r5QmqVUmdgsaUrGQ6uviHAD10Tnx4efYJWcbwZjgEwEVUvcvyes03l7eGIvRolCA9WBcvA%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260914T140528Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIARBIGYPT7HCRGY4TN%2F20260914%2Fap-southeast-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=66960ef78952c428132d81c30cf688e2c93fbe6dcf0e6ec7d58fb960504f571a";
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
                    batchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
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
            batchProcessing(importValidation.importId(), importValidation.importObjectName(), batchBuffer);
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


    private void batchProcessing(UUID importId, String importObjectName, List<ImportRow> rows) {
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

}


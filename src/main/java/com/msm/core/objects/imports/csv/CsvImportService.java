package com.msm.core.objects.imports.csv;

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
import com.msm.core.objects.imports.ReferenceProcessService;
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
public class CsvImportService {
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
//        String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
        String fileUrl = "https://msm-digiretail-dev-s3-data-001.s3.ap-southeast-1.amazonaws.com/bhc/masterData/v2order/csv/2026/09/16/v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789539157499_2306b6e2.csv?response-content-disposition=attachment%3B%20filename%3D%22v2order_b7e0a559-e6cd-4928-ad4a-ff7c661470dc_1789539157499_2306b6e2.csv%22%3B%20filename%2A%3DUTF-8%27%27Profile.csv&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEFkaDmFwLXNvdXRoZWFzdC0xIkgwRgIhAOZNBSJhTNKQDLZfqyeuIl3Zc%2BlQs8iiRT1dv4U1AV4wAiEA3sGVbGvorG9i6YBMPQshUt%2BjdEFmGoAFBiStUdkxKxEqmAQIIhAAGgwwNzE0MTgwMTkwNzAiDFBWeRrbi2hYa3w2%2BSr1A7mU8Ska%2B%2BrLeKigwwVo4CBLiZDXVBHVTF217A7zV2yOASseRHQdemymrdDqLIVkhRK46gbp6qiVQSZjcex1P5xhIVFZHciwTnW74c9rJaYiCielZ%2Fh6cCG71xNILBQbRjU0AhdFLqMA4X%2F%2BbBeHXl9tzUPyaU%2FBNGxDXgXjjcQHH13ObRd1Eu%2F2CLFm8cYOVUN4%2BqVZsHZcBi3jMhEv3LrFNQPqnzWqIFuuYkImOnu8mYhTlbrhxesYuWG%2FgwIIJqUxf1VG80dWO4OW1X%2BfJ03R38WZsEL7r6UYKEe%2FI1pf5Tp4TwW0HHrordJblTgogmTd6H0JVD74TCvy4gG8fpjwBvOY%2BjaCB6dpWi%2FhNRjq2b4l9GSJLhvWg5aD7ZCBEysn7QR2vhSJRczep6KqZ1RZzV1EIX9fGyJ5PWgGsujG8Ifbng0Ait%2BybgbDzFxu4Cl%2F%2F1XBolAmC7HR8bC6pETZ3WJthnex4%2B%2B1tw7dYXz4HZB3RKAU3tK1Iyl3kiy%2B9irTSgF6alExAA4ZcZNOGFgb5fSawVDQ5pgDSoduEmTtxxoCGuJGMO5MNoqR1MLLUAz0TtJF0d09VVVe2qqgPcWB%2F5eHrhAsZwetggvmA6mJtIV%2Flt%2B9fDUVYxGnb%2FVbHb3s3%2BLewQcsFsw3%2BN2kf%2BjAK9jifDD0%2FqzVBjqlAXLDJPUde0G9i78y9i1tnmEQ7IdPXKx1h2B3diqKCGnTBlp8mUqueaLuOBU9VO3V8u7B%2BhS5HtXHjmI%2FaJC%2BMcUKDCQAnMopxW0nQ0Qa79X4fdp77SaCfZQxeFg6oWyWH120Xbbz3dYZaDiG7myXkvWYMpvcKZLRNOQrbQbfvnQSyrGur0oRQU4sT6trNRDw5ap8xV0nnyUCqTKQNF%2BzBfH%2FaW2nTg%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260917T051201Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIARBIGYPT7DFNWSWBK%2F20260917%2Fap-southeast-1%2Fs3%2Faws4_request&X-Amz-Expires=3600&X-Amz-Signature=6aa03182bcad40378f3e7a247d96407f3bb7e19a5eb640df27bd881438d42462";
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

        ReadActionContext<CSVRecord> request = ReadActionContext.of(importValidation.importObjectName(), importId, importValidation.fileUrl(), rowConsumer);

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



//    public void generateErrorExcelStream(String fileUrl, UUID importHistoryId, OutputStream outputStream) {
//
//        List<Map<String, Object>> errors = internalObjectQueryRepository.findByCondition(
//                ImportErrorMeta.OBJECT_NAME,
//                ImportErrorMeta.IMPORT_ID.getField().eq(importHistoryId)
//        );
//
//        Map<Long, DataRecord> dataError = Utils.D.groupBy(
//                errors,
//                objectMap -> (Long) objectMap.get(ImportErrorMeta.ROW_NUMBER.getFieldName()),
//                DataRecord::ofNullable
//        );

//
//
//        // 2. Sử dụng SXSSFWorkbook để ghi streaming trực tiếp nhằm tối ưu RAM
//        try (SXSSFWorkbook targetWorkbook = new SXSSFWorkbook(100)) { // Giữ tối đa 100 dòng trên RAM
//
//            Sheet targetSheet = targetWorkbook.createSheet("Error Logs");
//
//            // Biến theo dõi dòng ghi hiện tại trong file mới
//            // Sử dụng mảng 1 phần tử hoặc đối tượng để có thể thay đổi giá trị bên trong Lambda Consumer
//            final int[] currentWriteRow = {0};
//
//            // 3. Gọi hàm readExcelStream CŨ CỦA BẠN để đọc file gốc bằng buffer từ S3
//            ImportHelper.readExcelStream(
//                    "importjob",
//                    fileUrl,
//                    64 * 1024, // Buffer size 64KB cho mạng
//                    100,       // rowCacheSize cho thư viện streaming reader
//                    rawRow -> {
//                        Row originalRow = rawRow.data();
//                        int totalCols = originalRow.getLastCellNum() > 0 ? originalRow.getLastCellNum() : 0;
//
//                        // Tạo Header cho file mới dựa vào dòng đầu tiên đọc được
//                        if (currentWriteRow[0] == 0) {
//                            Row headerRow = targetSheet.createRow(currentWriteRow[0]++);
//                            for (int i = 0; i < totalCols; i++) {
//                                Cell cell = originalRow.getSheet().getRow(0).getCell(i);
//                                headerRow.createCell(i).setCellValue(cell != null ? cell.getStringCellValue() : "");
//                            }
//                            // Thêm cột Errors ở cuối dòng Header
//                            headerRow.createCell(totalCols).setCellValue("Errors");
//                        }
//
//                        // Tạo dòng mới và copy dữ liệu cũ sang
//                        Row newRow = targetSheet.createRow(currentWriteRow[0]++);
//                        for (int i = 0; i < totalCols; i++) {
//                            Cell cell = originalRow.getCell(i);
//                            newRow.createCell(i).setCellValue(cell != null ? cell.getStringCellValue() : "");
//                        }
//
//                        // 4. Kiểm tra xem số dòng hiện tại (rawRow.getRowNum()) có lỗi trong DB hay không
//
//                        String errorMessage = dataError.get(originalRow.getRowNum());
//                        if (errorMessage != null) {
//                            newRow.createCell(totalCols).setCellValue(errorMessage);
//                        } else {
//                            newRow.createCell(totalCols).setCellValue("");
//                        }
//                    }
//            );
//
//            // 5. Ghi thẳng dữ liệu Excel ra OutputStream đã truyền vào (Luồng mạng của Response hoặc file tùy ý)
//            targetWorkbook.write(outputStream);
//            targetWorkbook.dispose(); // Xóa các file tạm đệm của SXSSFWorkbook trên đĩa
//            outputStream.flush();
//
//        } catch (IOException e) {
//            log.error("Lỗi khi xử lý ghi Excel Streaming trong Service: {}", e.getMessage(), e);
//            throw new RuntimeException("Không thể sinh file lỗi Excel", e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }



}


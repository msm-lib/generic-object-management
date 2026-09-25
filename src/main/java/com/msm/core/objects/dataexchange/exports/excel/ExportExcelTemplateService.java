package com.msm.core.objects.dataexchange.exports.excel;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.dataexchange.ColumnToAttributeMappingContext;
import com.msm.core.objects.dataexchange.exports.model.ColumnHeaderPath;
import com.msm.core.objects.exception.AttributeColumnMappingNotFoundException;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExportExcelTemplateService {
    private final S3Client s3Client;
    private final ActionExecutor actionExecutor;

    public byte[] getTemplateBytes(String bucketName, String templateKey) {
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(templateKey)
                        .build()
        );
        return objectBytes.asByteArray();
    }

//    public Map<Integer, String> extractColumnHeaderMap(byte[] templateBytes, UUID exportId, String objectName) {
//        Map<Integer, String> columnHeaderMap = null;
//
//        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(templateBytes);
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            for (Row row : sheet) {
//                ColumnToAttributeMappingContext<Row> mapperContext = ColumnToAttributeMappingContext.of(
//                        exportId,
//                        row.getRowNum(),
//                        objectName,
//                        row
//                );
//                ActionContext<ColumnToAttributeMappingContext<Row>> actionContext = ActionContext
//                        .<ColumnToAttributeMappingContext<Row>>builder()
//                        .resource(objectName)
//                        .action(ObjectActionNamed.Excel.Export.DETECT_COLUMN_HEADER_MAPPING)
//                        .payload(mapperContext)
//                        .build();
//
//                columnHeaderMap = actionExecutor.execute(actionContext);
//                if (Utils.CL.isNotEmpty(columnHeaderMap)) {
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error while mapping column header", e);
//            throw Lombok.sneakyThrow(e);
//        }
//
//        if (Utils.CL.isEmpty(columnHeaderMap)) {
//            throw new AttributeColumnMappingNotFoundException("Column header map is empty");
//        }
//        return columnHeaderMap;
//    }


    public Map<Integer, ColumnHeaderPath> extractColumnHeaderMap(byte[] templateBytes, UUID exportId, String objectName) {
        Map<Integer, ColumnHeaderPath> columnHeaderMap = null;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(templateBytes);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                ColumnToAttributeMappingContext<Row> mapperContext = ColumnToAttributeMappingContext.of(
                        exportId,
                        row.getRowNum(),
                        objectName,
                        row
                );
                ActionContext<ColumnToAttributeMappingContext<Row>> actionContext = ActionContext
                        .<ColumnToAttributeMappingContext<Row>>builder()
                        .resource(objectName)
                        .action(ObjectActionNamed.Excel.Export.DETECT_COLUMN_HEADER_MAPPING)
                        .payload(mapperContext)
                        .build();

                columnHeaderMap = actionExecutor.execute(actionContext);
                if (Utils.CL.isNotEmpty(columnHeaderMap)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error while mapping column header", e);
            throw Lombok.sneakyThrow(e);
        }

        if (Utils.CL.isEmpty(columnHeaderMap)) {
            throw new AttributeColumnMappingNotFoundException("Column header map is empty");
        }
        return columnHeaderMap;
    }

}

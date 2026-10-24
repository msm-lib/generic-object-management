package com.msm.core.objects.dataexchange.imports.s3;

import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.Condition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcelOriginalMultipartAsyncServiceTest {

    private final ExcelOriginalMultipartAsyncService service = new ExcelOriginalMultipartAsyncService(
            null, null, null, mock(S3FileUtils.class), null);

    @Test
    void errorsOfEachSheetAreWrittenInThatSheet() throws Exception {
        try (XSSFWorkbook workbook = workbook()) {
            service.writeErrors(workbook, List.of(
                    new ExcelOriginalMultipartAsyncService.SheetErrors("Order", Map.of(2L, error("bad order type"))),
                    new ExcelOriginalMultipartAsyncService.SheetErrors("orderitem", Map.of(1L, error("missing lineType")))));

            Sheet order = workbook.getSheet("Order");
            Sheet item = workbook.getSheet("OrderItem");
            assertThat(cell(order, 1)).isEmpty();
            assertThat(cell(order, 2)).isEqualTo("bad order type");
            assertThat(cell(item, 1)).isEqualTo("missing lineType");
            assertThat(cell(item, 2)).isEmpty();
        }
    }

    @Test
    void dependentSheetWithoutErrorsIsLeftAsItIs() throws Exception {
        try (XSSFWorkbook workbook = workbook()) {
            service.writeErrors(workbook, List.of(
                    new ExcelOriginalMultipartAsyncService.SheetErrors("Order", Map.of(1L, error("bad order"))),
                    new ExcelOriginalMultipartAsyncService.SheetErrors("OrderItem", Map.of())));

            assertThat(workbook.getSheet("OrderItem").getRow(1).getLastCellNum()).isEqualTo((short) 2);
        }
    }

    @Test
    void errorsOfOneRowShareItsCell() {
        ObjectQueryRepository repository = mock(ObjectQueryRepository.class);
        when(repository.findByCondition(eq(ImportErrorMeta.OBJECT_NAME), any(Condition.class))).thenReturn(List.of(
                errorRow(1, "orderId: cannot convert"),
                errorRow(1, "orderId loại dữ liệu không hợp lệ"),
                errorRow(1, "orderId loại dữ liệu không hợp lệ"),
                errorRow(2, "lineType is required")));
        ExcelOriginalMultipartAsyncService withRepository = new ExcelOriginalMultipartAsyncService(
                null, repository, null, mock(S3FileUtils.class), null);

        Map<Long, DataRecord> errors = withRepository.errorsByRow(UUID.randomUUID());

        assertThat(errors.get(1L).get(ImportErrorMeta.ERROR_MESSAGE))
                .isEqualTo("orderId: cannot convert; orderId loại dữ liệu không hợp lệ");
        assertThat(errors.get(2L).get(ImportErrorMeta.ERROR_MESSAGE)).isEqualTo("lineType is required");
    }

    private static Map<String, Object> errorRow(long rowNumber, String message) {
        return Map.of(ImportErrorMeta.ROW_NUMBER.getFieldName(), rowNumber, ImportErrorMeta.ERROR_MESSAGE.getFieldName(), message);
    }

    private static XSSFWorkbook workbook() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        for (String name : List.of("Order", "OrderItem")) {
            Sheet sheet = workbook.createSheet(name);
            for (int r = 0; r < 3; r++) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue(r == 0 ? "code" : name + r);
                row.createCell(1).setCellValue(r == 0 ? "name" : "x");
            }
        }
        return workbook;
    }

    private static DataRecord error(String message) {
        return DataRecord.of().with(ImportErrorMeta.ERROR_MESSAGE, message);
    }

    private static String cell(Sheet sheet, int row) {
        return sheet.getRow(row).getCell(2).getStringCellValue();
    }
}

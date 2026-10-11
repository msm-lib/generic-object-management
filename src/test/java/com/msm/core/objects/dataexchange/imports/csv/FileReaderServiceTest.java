package com.msm.core.objects.dataexchange.imports.csv;

import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.objects.exception.ImportConfigurationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FileReaderServiceTest {

    @Test
    void getSheetNamesReturnsWorkbookSheetsInOrder(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("orders.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            workbook.createSheet("Order").createRow(0).createCell(0).setCellValue("code");
            workbook.createSheet("OrderItem").createRow(0).createCell(0).setCellValue("orderId");
            workbook.write(out);
        }

        FileReaderService service = new FileReaderService(mock(ActionExecutor.class));

        assertThat(service.getSheetNames(file.toUri().toString(), 4096)).containsExactly("Order", "OrderItem");
    }

    @Test
    void getSheetReadsNamedSheetOrTheOnlySheet() {
        try (XSSFWorkbook twoSheets = new XSSFWorkbook(); XSSFWorkbook oneSheet = new XSSFWorkbook()) {
            twoSheets.createSheet("Order");
            twoSheets.createSheet("OrderItem");
            oneSheet.createSheet("Sheet1");

            assertThat(FileReaderService.getSheet(twoSheets, "OrderItem").getSheetName()).isEqualTo("OrderItem");
            assertThat(FileReaderService.getSheet(twoSheets, "orderitem").getSheetName()).isEqualTo("OrderItem");
            assertThat(FileReaderService.getSheet(twoSheets, null).getSheetName()).isEqualTo("Order");
            assertThat(FileReaderService.getSheet(oneSheet, "Contact").getSheetName()).isEqualTo("Sheet1");
            assertThatThrownBy(() -> FileReaderService.getSheet(twoSheets, "Contact"))
                    .isInstanceOf(ImportConfigurationException.class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

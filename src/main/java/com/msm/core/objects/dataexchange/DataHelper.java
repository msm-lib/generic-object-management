package com.msm.core.objects.dataexchange;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class DataHelper {
    private DataHelper(){}

    public static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();

            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    yield (long) value;
                }

                yield value;
            }

            case BOOLEAN -> cell.getBooleanCellValue();

            case FORMULA -> getFormulaCellValue(cell);

            case ERROR -> cell.getErrorCellValue();

            default -> null;
        };
    }


    public static Object getFormulaCellValue(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();

            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }

                double value = cell.getNumericCellValue();

                if (value == Math.floor(value)) {
                    yield (long) value;
                }

                yield value;
            }

            case BOOLEAN -> cell.getBooleanCellValue();

            case ERROR -> cell.getErrorCellValue();

            default -> null;
        };
    }

    public static boolean isColumnHeaderValid(ObjectMetadata objectMetadata, Map<Integer, String> columnHeaders, int totalColumn) {
        List<Attribute> attrs = objectMetadata.getAttributes();

        if(Utils.CL.isEmpty(columnHeaders)) {
            return false;
        }

        if (attrs == null || attrs.isEmpty()) {
            return true;
        }

        Set<String> columnNames = Set.copyOf(columnHeaders.values());
        long count = attrs.stream()
                .map(Attribute::getFieldName)
                .filter(columnNames::contains)
                .count();

        return count >= (totalColumn / 2);
    }

    public static boolean isColumnHeaderValid(ObjectMetadata objectMetadata, Map<Integer, String> columnHeaders, double requiredPercentage) {
        List<Attribute> attrs = objectMetadata.getAttributes();

        if(Utils.CL.isEmpty(columnHeaders)) {
            return false;
        }

        if (attrs == null || attrs.isEmpty()) {
            return false;
        }

        Set<String> columnNames = Set.copyOf(columnHeaders.values());
        long matchCount = attrs.stream()
                .map(Attribute::getFieldName)
                .filter(columnNames::contains)
                .count();

        double actualPercentage = ((double) matchCount / attrs.size()) * 100;

        return actualPercentage >= requiredPercentage;
    }

//    public static void readExcelStream(String importObjectName, UUID importId, String fileUrl, int bufferSize, int batchSize, Consumer<RawRow<Row>> consumer) {
//        Workbook workbook = null;
//        InputStream inputStream = null;
//        try {
//            URL url = URI.create(fileUrl).toURL();
//            inputStream = url.openStream();
//            workbook = StreamingReader.builder()
//                    .rowCacheSize(batchSize)           // Data hold in RAM (Buffer Size)
//                    .bufferSize(bufferSize)            // Buffer size of InputStream (Bytes)
//                    .open(inputStream);
//
//            Sheet sheet = workbook.getSheetAt(0);
//            Iterator<Row> rowIterator = sheet.iterator();
//            if (rowIterator.hasNext()) {
//                rowIterator.next();
//            }
//
//            while (rowIterator.hasNext()) {
//                Row row = rowIterator.next();
//                consumer.accept(
//                        new RawRow<>(row.getRowNum(), importObjectName, row)
//                );
//            }
//        } catch (IOException e) {
//            throw Lombok.sneakyThrow(e);
//        } finally {
//            if (Objects.nonNull(workbook)) {
//                try {
//                    workbook.close();
//                } catch (IOException e) {
//                    log.error("Error while close workbook: {}", e.getMessage(), e);
//                }
//            }
//            if (Objects.nonNull(inputStream)) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    log.error("Error while close inputStream: {}", e.getMessage(), e);
//                }
//            }
//        }
//    }
//
//    public static void readExcel(String importObjectName, String fileUrl, Consumer<RawRow<Row>> consumer) {
////        Workbook workbook = null;
////        InputStream inputStream = null;
//
//        try {
//            URL url = URI.create(fileUrl).toURL();
//            try (
//                    InputStream inputStream = url.openStream();
//                    Workbook workbook = WorkbookFactory.create(inputStream)
//            ) {
//
//                Sheet sheet = workbook.getSheetAt(0);
//                Iterator<Row> rowIterator = sheet.iterator();
//                if (rowIterator.hasNext()) {
//                    rowIterator.next();
//                }
//
//                while (rowIterator.hasNext()) {
//                    Row row = rowIterator.next();
//                    consumer.accept(
//                            new RawRow<>(row.getRowNum(), importObjectName, row)
//                    );
//                }
//
//            }
//        } catch (Exception e) {
//            throw Lombok.sneakyThrow(e);
//        } finally {
//
//        }
////        try (
////
////                InputStream inputStream = url.openStream();
////                Workbook workbook = WorkbookFactory.create(inputStream)
////        ){
//////            URL url = URI.create(fileUrl).toURL();
//////            inputStream = url.openStream();
//////            workbook = WorkbookFactory.create(inputStream);
////
////            Sheet sheet = workbook.getSheetAt(0);
////            Iterator<Row> rowIterator = sheet.iterator();
////            if (rowIterator.hasNext()) {
////                rowIterator.next();
////            }
////
////            while (rowIterator.hasNext()) {
////                Row row = rowIterator.next();
////                consumer.accept(
////                        new RawRow<>(row.getRowNum(), importObjectName, row)
////                );
////            }
////
////        } catch (IOException e) {
////            throw Lombok.sneakyThrow(e);
////        } finally {
////            if (Objects.nonNull(workbook)) {
////                try {
////                    workbook.close();
////                } catch (IOException e) {
////                    log.error("Error while close workbook: {}", e.getMessage(), e);
////                }
////            }
////            if (Objects.nonNull(inputStream)) {
////                try {
////                    inputStream.close();
////                } catch (IOException e) {
////                    log.error("Error while close inputStream: {}", e.getMessage(), e);
////                }
////            }
////        }
//    }
//
//    private void finishFileProcess(String importObjectName, UUID importId, FileType fileType, FileProcessStatus status) {
//
//        FileProcessedEventContext.of(importObjectName, importId, fileType, status);
//        ActionContext<ReadActionContext<Row>> actionRequest = ActionContext
//                .<ReadActionContext<Row>>builder()
//                .resource(importValidation.importObjectName())
//                .action(ObjectActionNamed.Excel.READ_FILE)
//                .payload(request)
//                .build();
//
//        actionExecutor.execute(actionRequest);
//    }
    public static List<String> arrayParser(String resource) {
        if (Utils.STR.isBlank(resource)) {
            return Utils.CL.newArrayList();
        }
        return  Arrays.asList(resource.replaceAll("[{}]", "").split(","));
    }
}

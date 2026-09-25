package com.msm.core.objects.dataexchange.imports;

import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.InsertDataResult;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import lombok.RequiredArgsConstructor;
import org.jooq.exception.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLDataException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ImportDataProcessor {

    private final ImportDataTransactionExecutor transactionExecutor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processUpdateBatch(
            UUID importId,
            String objectName,
            List<DataRecord> records,
            List<DataRecord> errors,
            List<InsertDataResult> results
    ) {
        if (records.isEmpty()) {
            return 0;
        }

        try {
            transactionExecutor.updateBatch(objectName, records);
//            internalObjectQueryRepository.update(
//                    objectName,
//                    records.stream()
//                            .map(r -> r.get(ImportStagingMeta.DATA))
//                            .collect(Collectors.toList())
//            );

            return records.size();

        } catch (Exception batchException) {

            /*
             * Batch failed.
             *
             * Don't immediately mark all records as failed.
             * Retry one-by-one to identify the actual bad records.
             */
            int success = 0;

            for (DataRecord record : records) {
                try {
                    transactionExecutor.updateSingle(objectName, record);
                    success++;
                } catch (Exception rowException) {
                    String message = resolveErrorMessage(rowException);
                    errors.add(buildError(
                            importId,
                            record,
                            resolveErrorType(rowException),
                            resolveErrorCode(rowException),
                            message
                    ));

                    results.add(buildErrorResult(
                            record,
                            message
                    ));
                }
            }

            /*
             * In case batch exception happened but every individual
             * row succeeded, the original batch exception was probably
             * related to the batch itself.
             */
            if (success == records.size()) {
                // Optional: log batchException
            }

            return success;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processInsertBatch(
            UUID importId,
            String objectName,
            List<DataRecord> records,
            List<DataRecord> errors,
            List<InsertDataResult> results
    ) {
        if (records.isEmpty()) {
            return 0;
        }

        try {
//            internalObjectQueryRepository.insertBatch(
//                    objectName,
//                    records.stream()
//                            .map(r -> r.get(ImportStagingMeta.DATA))
//                            .collect(Collectors.toList())
//            );
            transactionExecutor.insertBatch(objectName, records);
            return records.size();

        } catch (Exception batchException) {

            int success = 0;

            /*
             * Fallback to row-by-row insertion so that one bad
             * record does not cause the entire import to fail.
             */
            for (DataRecord record : records) {
                try {
//                    internalObjectQueryRepository.insertBatch(
//                            objectName,
//                            List.of(record.get(ImportStagingMeta.DATA))
//                    );
                    transactionExecutor.insertSingle(objectName, record);
                    success++;

                } catch (Exception rowException) {
                    String message = resolveErrorMessage(rowException);
                    errors.add(buildError(
                            importId,
                            record,
                            resolveErrorType(rowException),
                            resolveErrorCode(rowException),
                            message
                    ));

                    results.add(buildErrorResult(
                            record,
                            message
                    ));
                }
            }

            return success;
        }
    }

    private String resolveErrorType(Exception e) {

        Throwable root = getRootCause(e);

        if (root instanceof SQLIntegrityConstraintViolationException) {
            return "DATABASE_CONSTRAINT";
        }

        if (root instanceof SQLDataException) {
            return "INVALID_DATA";
        }

        if (root instanceof SQLTimeoutException) {
            return "DATABASE_TIMEOUT";
        }

        if (root instanceof SQLNonTransientConnectionException) {
            return "DATABASE_CONNECTION";
        }

        if (root instanceof DataAccessException) {
            return "DATABASE";
        }

        return "IMPORT_ERROR";
    }

    private String resolveErrorCode(Exception e) {

        Throwable root = getRootCause(e);
        if (root instanceof SQLIntegrityConstraintViolationException) {
            String message = root.getMessage();

            if (message != null) {
                String lower = message.toLowerCase();

                if (lower.contains("duplicate")
                        || lower.contains("unique")) {
                    return "DUPLICATE_KEY";
                }

                if (lower.contains("foreign key")) {
                    return "FOREIGN_KEY_VIOLATION";
                }

                if (lower.contains("not-null")
                        || lower.contains("not null")) {
                    return "NOT_NULL_VIOLATION";
                }
            }

            return "CONSTRAINT_VIOLATION";
        }

        if (root instanceof SQLDataException) {
            String message = root.getMessage();

            if (message != null) {
                String lower = message.toLowerCase();

                if (lower.contains("too long")
                        || lower.contains("value too large")
                        || lower.contains("data too long")) {
                    return "DATA_TOO_LONG";
                }

                if (lower.contains("numeric")
                        || lower.contains("number")) {
                    return "INVALID_NUMBER";
                }

                if (lower.contains("date")
                        || lower.contains("timestamp")) {
                    return "INVALID_DATE";
                }
            }

            return "INVALID_DATA";
        }

        if (root instanceof SQLTimeoutException) {
            return "DATABASE_TIMEOUT";
        }

        if (root instanceof SQLNonTransientConnectionException) {
            return "DATABASE_CONNECTION_ERROR";
        }

        return "IMPORT_ERROR";
    }

    public String resolveErrorMessage(Exception e) {

        Throwable root = getRootCause(e);
        String message = root.getMessage();
        if (root instanceof SQLIntegrityConstraintViolationException) {
            if (message != null) {
                String lower = message.toLowerCase();

                if (lower.contains("duplicate")
                        || lower.contains("unique")) {
                    return "The record already exists. Please check the unique/identity fields.";
                }

                if (lower.contains("foreign key")) {
                    return "The record references data that does not exist. Please check the related fields.";
                }

                if (lower.contains("not-null")
                        || lower.contains("not null")) {
                    return "A required field is missing. Please provide all mandatory fields.";
                }
            }

            return "The record violates a database constraint.";
        }

        if (root instanceof SQLDataException) {

            if (message != null) {
                String lower = message.toLowerCase();

                if (lower.contains("too long")
                        || lower.contains("value too large")
                        || lower.contains("data too long")) {
                    return "One or more values exceed the allowed field length.";
                }

                if (lower.contains("numeric")
                        || lower.contains("number")) {
                    return "One or more values have an invalid number format.";
                }

                if (lower.contains("date")
                        || lower.contains("timestamp")) {
                    return "One or more values have an invalid date or time format.";
                }
            }

            return "The provided data is invalid.";
        }

        if (root instanceof SQLTimeoutException) {
            return "The database operation timed out. Please try again later.";
        }

        if (root instanceof SQLNonTransientConnectionException) {
            return "Unable to connect to the database. Please try again later.";
        }

        if (message == null || message.isBlank()) {
            return "An unexpected error occurred while processing the record.";
        }

        return message;
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }

    public DataRecord buildError(
            UUID importId,
            DataRecord stagingRecord,
            String errorType,
            String errorCode,
            String message
    ) {
        return DataRecord.of()
                .with(ImportErrorMeta.IMPORT_ID, importId)
                .with(
                        ImportErrorMeta.ROW_NUMBER,
                        stagingRecord.get(ImportStagingMeta.ROW_NUMBER)
                )
                .with(
                        ImportErrorMeta.ERROR_TYPE,
                        errorType
                )
                .with(
                        ImportErrorMeta.ERROR_MESSAGE,
                        message
                )
                .with(
                        ImportErrorMeta.ERROR_CODE,
                        errorCode
                )
                .with(
                        ImportStagingMeta.DATA,
                        stagingRecord.get(ImportStagingMeta.DATA)
                );
    }

    public InsertDataResult buildErrorResult(
            DataRecord record,
            String message
    ) {
        return new InsertDataResult(
                0,
                ImportStatus.COMPLETED_WITH_ERRORS,
                message,
                record.getValues()
        );
    }
}

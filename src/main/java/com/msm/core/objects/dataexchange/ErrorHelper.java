package com.msm.core.objects.dataexchange;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.io.IOException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ErrorHelper {

    public static String resolveErrorMessage(Exception exception) {
        Throwable root = getRootCause(exception);

        if (root instanceof NumberFormatException) {
            return "The value has an invalid number format.";
        }

        if (root instanceof DateTimeParseException) {
            return "The value has an invalid date or time format.";
        }

        if (root instanceof ParseException) {
            return "The value could not be parsed.";
        }

        if (root instanceof IOException) {
            return "The file could not be read. Please check the file and try again.";
        }

        // Apache POI
        if (root instanceof InvalidFormatException) {
            return "The uploaded file has an invalid or unsupported Excel format.";
        }

        if (root instanceof POIXMLException) {
            return "The Excel file is corrupted or could not be read.";
        }

        // =========================================================
        // Validation
        // =========================================================

        if (root instanceof IllegalArgumentException) {
            return resolveIllegalArgumentMessage(root);
        }

        if (root instanceof IllegalStateException) {
            return "The data could not be processed because it is in an invalid state.";
        }

        // =========================================================
        // Database constraint
        // =========================================================

        if (root instanceof SQLIntegrityConstraintViolationException) {
            return resolveConstraintMessage(root);
        }

        // PostgreSQL specific constraint exception
//        if (root instanceof PSQLException) {
//            String message = root.getMessage();
//
//            if (message != null) {
//                String lower = message.toLowerCase(Locale.ROOT);
//
//                if (lower.contains("duplicate key")
//                        || lower.contains("unique constraint")
//                        || lower.contains("duplicate")) {
//                    return "The record already exists. Please check the unique or identity fields.";
//                }
//
//                if (lower.contains("foreign key constraint")
//                        || lower.contains("violates foreign key")) {
//                    return "The record references data that does not exist. Please check the related fields.";
//                }
//
//                if (lower.contains("not-null constraint")
//                        || lower.contains("null value in column")) {
//                    return "A required field is missing. Please provide all mandatory fields.";
//                }
//
//                if (lower.contains("check constraint")) {
//                    return "The data does not satisfy the required validation rules.";
//                }
//
//                if (lower.contains("invalid input syntax")) {
//                    return "One or more values have an invalid format.";
//                }
//
//                if (lower.contains("value too long")
//                        || lower.contains("character varying")
//                        || lower.contains("too long")) {
//                    return "One or more values exceed the allowed field length.";
//                }
//
//                if (lower.contains("numeric field overflow")
//                        || lower.contains("out of range")) {
//                    return "One or more numeric values are outside the allowed range.";
//                }
//
//                if (lower.contains("current transaction is aborted")) {
//                    return "The database operation could not be completed because the transaction failed.";
//                }
//            }
//        }

        if (root instanceof SQLException sqlException) {
            return resolveSqlErrorMessage(sqlException);
        }

        // =========================================================
        // SQL data errors
        // =========================================================

        if (findCause(exception, SQLDataException.class) != null) {
            return resolveSqlDataExceptionMessage(root);
        }

        if (root instanceof DataIntegrityViolationException) {
            return "The data violates a database constraint.";
        }

        // =========================================================
        // Database connectivity / timeout
        // =========================================================

        if (findCause(exception, SQLTimeoutException.class) != null) {
            return "The database operation timed out. Please try again later.";
        }

        if (root instanceof QueryTimeoutException) {
            return "The database operation timed out. Please try again later.";
        }

        if (findCause(exception, SQLNonTransientConnectionException.class) != null) {
            return "Unable to connect to the database. Please try again later.";
        }

        if (findCause(exception, SQLTransientConnectionException.class) != null) {
            return "The database connection is temporarily unavailable. Please try again later.";
        }

        if (root instanceof CannotGetJdbcConnectionException) {
            return "Unable to connect to the database. Please try again later.";
        }

        if (root instanceof DataAccessResourceFailureException) {
            return "The database is temporarily unavailable. Please try again later.";
        }

        // =========================================================
        // Spring DataAccessException
        // =========================================================

        if (root instanceof BadSqlGrammarException) {
            return "The data could not be saved because of a database error.";
        }

        if (root instanceof UncategorizedSQLException) {
            return "The data could not be saved because of a database error.";
        }

        if (root instanceof DataAccessException) {
            return "The data could not be saved because of a database error.";
        }

        if (root instanceof org.jooq.exception.DataAccessException) {
            return "The data could not be saved because of a database error.";
        }

        // =========================================================
        // Fallback
        // =========================================================

        String message = root.getMessage();

        if (message == null || message.isBlank()) {
            return "An unexpected error occurred while processing the record.";
        }

        /*
         * Only return the original message if it is considered safe.
         * Otherwise, hide internal implementation details.
         */
        return sanitizeErrorMessage(message);
    }

    private static String sanitizeErrorMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        /*
         * Do not expose internal database / SQL information.
         */
        if (lower.contains("select ")
                || lower.contains("insert into ")
                || lower.contains("update ")
                || lower.contains("delete from ")
                || lower.contains("jdbc")
                || lower.contains("hibernate")
                || lower.contains("postgresql")
                || lower.contains("psql")
                || lower.contains("sqlstate")
                || lower.contains("constraint")) {

            return "The data could not be processed.";
        }

        return message;
    }

    private static Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current;
    }
    private static String resolveConstraintMessage(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null) {
            return "The data violates a database constraint.";
        }

        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("duplicate")
                || lower.contains("unique")) {
            return "The record already exists. Please check the unique or identity fields.";
        }

        if (lower.contains("foreign key")) {
            return "The record references data that does not exist. Please check the related fields.";
        }

        if (lower.contains("not-null")
                || lower.contains("not null")) {
            return "A required field is missing. Please provide all mandatory fields.";
        }

        if (lower.contains("check constraint")) {
            return "The data does not satisfy the required validation rules.";
        }

        return "The data violates a database constraint.";
    }

    private static String resolveSqlDataExceptionMessage(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null) {
            return "The provided data is invalid.";
        }

        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("too long")
                || lower.contains("value too large")
                || lower.contains("data too long")
                || lower.contains("character varying")) {
            return "One or more values exceed the allowed field length.";
        }

        if (lower.contains("numeric")
                || lower.contains("number")
                || lower.contains("invalid input syntax for type integer")
                || lower.contains("invalid input syntax for type numeric")) {
            return "One or more values have an invalid number format.";
        }

        if (lower.contains("date")
                || lower.contains("timestamp")
                || lower.contains("time")) {
            return "One or more values have an invalid date or time format.";
        }

        if (lower.contains("out of range")
                || lower.contains("overflow")) {
            return "One or more values are outside the allowed range.";
        }

        return "The provided data is invalid.";
    }

    private static String resolveIllegalArgumentMessage(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return "The provided data is invalid.";
        }

        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("required")
                || lower.contains("must not be null")
                || lower.contains("cannot be null")) {
            return "A required field is missing.";
        }

        if (lower.contains("invalid")
                || lower.contains("unsupported")) {
            return "The provided value is invalid.";
        }

        return "The provided data is invalid.";
    }

    private static String resolveSqlErrorMessage(SQLException exception) {
        return switch (exception.getSQLState()) {
            case "23505" ->
                    "The record already exists. Please check the unique or identity fields.";

            case "23503" ->
                    "The record references data that does not exist. Please check the related fields.";

            case "23502" ->
                    "A required field is missing. Please provide all mandatory fields.";

            case "23514" ->
                    "The data does not satisfy the required validation rules.";

            case "22001" ->
                    "One or more values exceed the allowed field length.";

            case "22003" ->
                    "One or more numeric values are outside the allowed range.";

            case "22007", "22008" ->
                    "One or more values have an invalid date or time format.";

            case "22P02" ->
                    "One or more values have an invalid format.";

            default ->
                    "The data could not be saved because of a database error.";
        };
    }

    private static  <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }

            current = current.getCause();
        }

        return null;
    }


    private String resolveErrorCode(Exception exception) {
        Throwable root = getRootCause(exception);

        if (root instanceof SQLException sqlException) {
            String sqlState = sqlException.getSQLState();

            if (sqlState != null) {
                return switch (sqlState) {
                    case "23505" -> "DUPLICATE_KEY";
                    case "23503" -> "FOREIGN_KEY_VIOLATION";
                    case "23502" -> "NOT_NULL_VIOLATION";
                    case "23514" -> "CHECK_VIOLATION";
                    case "22001" -> "DATA_TOO_LONG";
                    case "22003" -> "NUMERIC_VALUE_OUT_OF_RANGE";
                    case "22007" -> "INVALID_DATETIME";
                    case "22P02" -> "INVALID_DATA_FORMAT";
                    default -> "DATABASE_ERROR";
                };
            }
        }

        return "IMPORT_ERROR";
    }
}

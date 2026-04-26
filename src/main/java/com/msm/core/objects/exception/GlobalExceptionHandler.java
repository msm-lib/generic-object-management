package com.msm.core.objects.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.msm.core.commons.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleBindException(MethodArgumentNotValidException exception) {
        Set<String> errors = new HashSet<>();
        if (exception.hasErrors()) {
            errors = exception.getAllErrors().stream().map(ObjectError::getDefaultMessage).collect(Collectors.toSet());
        }
        log.error("An error occurred ", exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, String.join(", ", errors)));
    }

    @ExceptionHandler(value = Errors.class)
    public ResponseEntity<Object> handleApiErrors(Errors exception) {
        log.error("An error occurred ", exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.create(exception.getDetails().stream().toList()));
    }

    @ExceptionHandler(value = Exception.class)
    protected ResponseEntity<Object> handleException(Exception exception) {
        log.error("An error occurred ", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.create(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
        log.error("An error occurred ", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.create(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleRuntimeException(HttpMessageNotReadableException exception) {
        log.error("An error occurred ", exception);
        String errorDetails = getStringMessage(exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, errorDetails));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.error("An error occurred ", exception);
        String parameterName = exception.getName();
        String requiredType = Objects.nonNull(exception.getRequiredType()) ? exception.getRequiredType().getSimpleName() : "Unknown";
        Object value = exception.getValue();
        String errorMessage = String.format("Parameter '%s' expects type '%s' but received value '%s'", parameterName, requiredType, value);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, errorMessage));
    }

    private String getStringMessage(HttpMessageNotReadableException exception) {
        String errorDetails = "Unknown";
        Throwable throwable = exception.getCause();
        if (throwable instanceof JsonMappingException) {
            JsonMappingException jsonMappingException = (JsonMappingException) exception.getCause();
            errorDetails = String.format("Invalid input format for the field: '%s'", getFieldName(jsonMappingException));
        }
        return errorDetails;
    }

    String getFieldName(JsonMappingException invalidFormatException) {
        List<JsonMappingException.Reference> references = invalidFormatException
                .getPath()
                .stream()
                .filter(reference -> Objects.nonNull(reference.getFieldName()))
                .toList();

        return Utils.CL.isNotEmpty(references) ? references.getLast().getFieldName() : "Unknown";
    }
}

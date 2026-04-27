package com.msm.core.objects.exception;


import com.msm.core.exceptions.GenericBaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

//@Slf4j
//@RestControllerAdvice
//@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler0 {

//    @ExceptionHandler(value = GenericBaseException.class)
//    public ResponseEntity<Object> handleApiErrors(GenericBaseException exception) {
//        log.error("An error occurred ", exception);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiErrorResponse.create(List.of(ErrorDetail.create(exception.getCode(), exception.getMessage()))));
//    }


//    @ExceptionHandler(value = MethodArgumentNotValidException.class)
//    public ResponseEntity<Object> handleBindException(MethodArgumentNotValidException exception) {
//        Set<String> errors = new HashSet<>();
//        if (exception.hasErrors()) {
//            errors = exception.getAllErrors().stream().map(ObjectError::getDefaultMessage).collect(Collectors.toSet());
//        }
//        log.error("An error occurred ", exception);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, String.join(", ", errors)));
//    }
//
//    @ExceptionHandler(value = Errors.class)
//    public ResponseEntity<Object> handleApiErrors(Errors exception) {
//        log.error("An error occurred ", exception);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiErrorResponse.create(exception.getDetails().stream().toList()));
//    }
//
//    @ExceptionHandler(value = Exception.class)
//    protected ResponseEntity<Object> handleException(Exception exception) {
//        log.error("An error occurred ", exception);
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiErrorResponse.create(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
//    }
//
//    @ExceptionHandler(value = RuntimeException.class)
//    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
//        log.error("An error occurred ", exception);
//
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiErrorResponse.create(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
//    }
//
//    @ExceptionHandler(value = HttpMessageNotReadableException.class)
//    public ResponseEntity<Object> handleRuntimeException(HttpMessageNotReadableException exception) {
//        log.error("An error occurred ", exception);
//        String errorDetails = getStringMessage(exception);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, errorDetails));
//    }
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
//        log.error("An error occurred ", exception);
//        String parameterName = exception.getName();
//        String requiredType = Objects.nonNull(exception.getRequiredType()) ? exception.getRequiredType().getSimpleName() : "Unknown";
//        Object value = exception.getValue();
//        String errorMessage = String.format("Parameter '%s' expects type '%s' but received value '%s'", parameterName, requiredType, value);
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ApiErrorResponse.create(ErrorCode.INVALID_INPUT_FIELD, errorMessage));
//    }
//
//    private String getStringMessage(HttpMessageNotReadableException exception) {
//        String errorDetails = "Unknown";
//        Throwable throwable = exception.getCause();
//        if (throwable instanceof JsonMappingException) {
//            JsonMappingException jsonMappingException = (JsonMappingException) exception.getCause();
//            errorDetails = String.format("Invalid input format for the field: '%s'", getFieldName(jsonMappingException));
//        }
//        return errorDetails;
//    }
//
//    String getFieldName(JsonMappingException invalidFormatException) {
//        List<JsonMappingException.Reference> references = invalidFormatException
//                .getPath()
//                .stream()
//                .filter(reference -> Objects.nonNull(reference.getFieldName()))
//                .toList();
//
//        return Utils.CL.isNotEmpty(references) ? references.getLast().getFieldName() : "Unknown";
//    }
}

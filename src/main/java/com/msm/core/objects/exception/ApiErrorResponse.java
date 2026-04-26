package com.msm.core.objects.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@ToString
public class ApiErrorResponse {

    private final List<ErrorDetail> errors;

    public static ApiErrorResponse create(String code, String message) {
        return create(ErrorDetail.create(code, message));
    }

    public static ApiErrorResponse create(ErrorDetail errorDetail) {
        return create(List.of(errorDetail));
    }

    public static ApiErrorResponse create(List<ErrorDetail> errorDetails) {
        return new ApiErrorResponse(errorDetails);
    }
}

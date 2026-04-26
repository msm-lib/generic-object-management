package com.msm.core.objects.exception;

import com.msm.core.commons.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class Errors extends RuntimeException {
    private final List<ErrorDetail> details;

    public static Errors throwException(ErrorDetail detail) {
        return new Errors(List.of(detail));
    }

    public static Errors throwException(List<ErrorDetail> details) {
        return new Errors(details);
    }

    public static Errors throwException(ServiceErrorEnum details) {
        return throwException(List.of(ErrorDetail.create(details.getCode(), details.getMessage())));
    }

    public static Errors throwException(ServiceErrorEnum details, Object... arg) {
        return throwException(List.of(ErrorDetail.create(details.getCode(), Utils.STR.format(details.getMessage(), arg))));
    }
}

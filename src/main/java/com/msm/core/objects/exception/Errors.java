package com.msm.core.objects.exception;

import com.msm.core.commons.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public String getMessage() {
        return "Exception: " + super.getMessage() + " | details=" + details;
    }

    @Override
    public String toString() {
        String detailStr;

        if (details == null || details.isEmpty()) {
            detailStr = "[]";
        } else {
            detailStr = details.stream()
                    .map(d -> String.format(
                            "{code='%s', message='%s'}",
                            d.getCode(),
                            d.getMessage()
                    ))
                    .collect(Collectors.joining(", "));
        }

        return "Errors{" +
                "details=[" + detailStr + "]" +
                '}';
    }
}

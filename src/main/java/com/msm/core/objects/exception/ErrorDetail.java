package com.msm.core.objects.exception;

import com.msm.core.commons.Utils;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Getter
@Builder
@RequiredArgsConstructor
public class ErrorDetail extends BaseErrorDetail implements Serializable {
    private final String code;
    private final String message;

    public String getCode() {
        return Utils.STR.format(FULL_ERROR_CODE, getTenantId(), getSystemIdentifier(), getServiceIdentifier(), code);
    }

    public static ErrorDetail create(String code, String message) {

        return new ErrorDetail(code, message);
    }

    public static ErrorDetail create(ServiceErrorEnum serviceErrorEnum) {
        return new ErrorDetail(serviceErrorEnum.getCode(), serviceErrorEnum.getMessage());
    }

    public static ErrorDetail create(ServiceErrorEnum serviceErrorEnum, Object... arg) {
        return ErrorDetail
                .builder()
                .code(serviceErrorEnum.getCode())
                .message(Utils.STR.format(serviceErrorEnum.getMessage(), arg))
                .build();
    }
}

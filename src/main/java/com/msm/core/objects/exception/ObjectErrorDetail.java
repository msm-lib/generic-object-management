package com.msm.core.objects.exception;

import com.msm.core.exceptions.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Map;

@ToString
@Getter
@Builder
@RequiredArgsConstructor
public class ObjectErrorDetail {
    private final ErrorCode code;
    private final Map<String, Object> params;
    private final String message;

    public static ObjectErrorDetail create(ErrorCode code, Map<String, Object> params, String message) {
        return new ObjectErrorDetail(code, params, message);
    }
}

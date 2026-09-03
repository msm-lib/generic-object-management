package com.msm.core.objects.exception;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class ObjectValidateException extends RuntimeException {

    private final String objectName;
    private final List<ObjectErrorDetail> details;

    public ObjectValidateException(String objectName, List<ObjectErrorDetail> details) {
        this.objectName = objectName;
        this.details = details;
    }

    public ObjectValidateException(String objectName, List<ObjectErrorDetail> details, Throwable cause) {
        this(objectName, details);
        this.initCause(cause);
    }

    public ObjectValidateException(String objectName, ObjectErrorDetail detail) {
        this(objectName, List.of(detail));
    }

    public ObjectValidateException(String objectName, ObjectErrorDetail detail, Throwable cause) {
        this(objectName, List.of(detail), cause);
    }
}

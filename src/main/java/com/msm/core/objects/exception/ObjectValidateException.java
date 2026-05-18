package com.msm.core.objects.exception;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class ObjectValidateException extends RuntimeException {

    private final List<ObjectErrorDetail> details;

    public ObjectValidateException(List<ObjectErrorDetail> details) {
        this.details = details;
    }

    public ObjectValidateException(List<ObjectErrorDetail> details, Throwable cause) {
        this(details);
        this.initCause(cause);
    }

    public ObjectValidateException(ObjectErrorDetail detail) {
        this.details = List.of(detail);
    }

    public ObjectValidateException(ObjectErrorDetail detail, Throwable cause) {
        this(detail);
        this.initCause(cause);
    }

    public static ObjectValidateException throwException(ObjectErrorDetail detail) {
        return new ObjectValidateException(List.of(detail));
    }

    public static ObjectValidateException throwException(ObjectErrorDetail detail, Throwable cause) {
        return new ObjectValidateException(detail, cause);
    }

    public static ObjectValidateException throwException(List<ObjectErrorDetail> details) {
        return new ObjectValidateException(details);
    }

    public static ObjectValidateException throwException(List<ObjectErrorDetail> details, Throwable cause) {
        return new ObjectValidateException(details, cause);
    }
}

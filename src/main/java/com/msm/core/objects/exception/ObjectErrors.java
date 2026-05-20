package com.msm.core.objects.exception;

import com.msm.core.exceptions.CommonErrors;
import com.msm.core.exceptions.ObjectNotFoundException;

import java.util.List;

public class ObjectErrors {
    public static ObjectValidateException validateException(List<ObjectErrorDetail> errors) {
        return new ObjectValidateException(errors);
    }

    public static ObjectValidateException validateException(List<ObjectErrorDetail> errors, Throwable cause) {
        return new ObjectValidateException(errors, cause);
    }

    public static ObjectNotFoundException notFound(String name) {
        return CommonErrors.objectNotFound(name);
    }

    public static PayloadInvalidException payloadInvalidException(String name, String msg, Throwable cause) {
        return new PayloadInvalidException(name, msg, cause);
    }

    public static CreateInstanceException createInstanceException(String msg, Throwable cause) {
        return new CreateInstanceException(msg, cause);
    }
}

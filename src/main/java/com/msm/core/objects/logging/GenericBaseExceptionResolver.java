package com.msm.core.objects.logging;

import com.msm.core.exceptions.GenericBaseException;
import org.springframework.http.HttpStatus;

/**
 * Default {@link IntegrationErrorResolver} for the lib-owned {@link GenericBaseException} family.
 * Runs last (lowest precedence) so service-specific resolvers can take priority. This is the only
 * exception mapping the reusable logging package can own, since {@code GenericBaseException} is a
 * library type.
 */

public class GenericBaseExceptionResolver implements IntegrationErrorResolver {

    @Override
    public boolean supports(Throwable ex) {
        return ex instanceof GenericBaseException;
    }

    @Override
    public void resolve(Throwable ex, IntegrationLogData.IntegrationLogDataBuilder builder) {
        GenericBaseException exception = (GenericBaseException) ex;
        builder.statusCode(HttpStatus.BAD_REQUEST.value())
                .errorCode(exception.getCode() != null ? exception.getCode().getCode() : null)
                .errorMessage(exception.getMessage());
    }
}

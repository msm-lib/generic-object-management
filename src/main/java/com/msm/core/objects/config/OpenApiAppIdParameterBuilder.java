package com.msm.core.objects.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class OpenApiAppIdParameterBuilder implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation,
                               HandlerMethod handlerMethod) {
        Parameter xTenantParam = new Parameter()
                .in("header")
                .name("X-Tenant")
                .description("Tenant code. Default: bhc")
                .required(true)
                .schema(new StringSchema().example("bhc"));

        operation.addParametersItem(xTenantParam);

        return operation;
    }
}

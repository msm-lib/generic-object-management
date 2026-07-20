package com.msm.core.objects.service.imports.resolver.impl;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.imports.resolver.strategy.AbstractObjectAttributeRefResolver;

import java.util.List;

public class PaymentTermIdRefResolver extends AbstractObjectAttributeRefResolver {

    public PaymentTermIdRefResolver(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    public String sourceObject() {
        return "profile";
    }

    @Override
    public String targetObject() {
        return "paymentterm";
    }

    @Override
    public String sourceAttribute() {
        return "paymentTermId";
    }


    @Override
    protected List<String> returnFields() {
        return List.of(
                "id",
                "code",
                "name",
                "date"
        );
    }
}
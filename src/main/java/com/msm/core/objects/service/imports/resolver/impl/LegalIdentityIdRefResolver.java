package com.msm.core.objects.service.imports.resolver.impl;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.imports.resolver.strategy.AbstractObjectAttributeRefResolver;

import java.util.List;

public class LegalIdentityIdRefResolver extends AbstractObjectAttributeRefResolver {

    public LegalIdentityIdRefResolver(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    public String sourceObject() {
        return "profile";
    }

    @Override
    public String targetObject() {
        return "organization";
    }

    @Override
    public String sourceAttribute() {
        return "legalIdentityId";
    }


    @Override
    protected List<String> returnFields() {
        return List.of(
                "id",
                "code",
                "name",
                "fax",
                "email",
                "nameEn",
                "website",
                "mainPhone",
                "billAddress",
                "mainAddress",
                "shipAddress",
                "allowSellOos",
                "isWmsEnabled",
                "legalIdentityName"
        );
    }
}
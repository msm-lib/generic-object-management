package com.msm.core.objects.service.imports.resolver.impl;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.imports.resolver.strategy.AbstractObjectAttributeRefResolver;

import java.util.List;


public class AccountSiteRefAccountResolver extends AbstractObjectAttributeRefResolver {

    public AccountSiteRefAccountResolver(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    public String sourceObject() {
        return "accountsite";
    }

    @Override
    public String targetObject() {
        return "account";
    }

    @Override
    public String sourceAttribute() {
        return "accountId";
    }


    @Override
    protected List<String> returnFields() {
        return List.of(
                "id",
                "code",
                "name",
                "email",
                "partyId",
                "partyNumber",
                "taxRegistration",
                "profileRelationshipType"
        );
    }
}

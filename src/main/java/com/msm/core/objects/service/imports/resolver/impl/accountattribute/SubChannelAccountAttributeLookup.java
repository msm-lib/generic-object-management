package com.msm.core.objects.service.imports.resolver.impl.accountattribute;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;

public class SubChannelAccountAttributeLookup extends AbstractAccountAttributeLookup {
    public SubChannelAccountAttributeLookup(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    String getAttributeType() {
        return "SUB_CHANNEL";
    }

    @Override
    public String sourceObject() {
        return "accountsite";
    }

    @Override
    public String sourceAttribute() {
        return "subChannelId";
    }

    @Override
    public String targetObject() {
        return "accountattribute";
    }
}

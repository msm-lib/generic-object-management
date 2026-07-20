package com.msm.core.objects.service.imports.resolver.impl.accountattribute;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;

public class ChannelAccountAttributeLookup extends AbstractAccountAttributeLookup {
    public ChannelAccountAttributeLookup(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    String getAttributeType() {
        return "CHANNEL";
    }

    @Override
    public String sourceObject() {
        return "accountsite";
    }

    @Override
    public String sourceAttribute() {
        return "channelId";
    }

    @Override
    public String targetObject() {
        return "accountattribute";
    }
}

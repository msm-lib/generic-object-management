package com.msm.core.objects.service.imports.resolver.impl.accountattribute;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;

public class ChannelDetailAccountAttributeLookup extends AbstractAccountAttributeLookup {
    public ChannelDetailAccountAttributeLookup(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }

    @Override
    String getAttributeType() {
        return "CHANNEL_DETAIL";
    }

    @Override
    public String sourceObject() {
        return "accountsite";
    }

    @Override
    public String sourceAttribute() {
        return "channelDetailId";
    }

    @Override
    public String targetObject() {
        return "accountattribute";
    }
}

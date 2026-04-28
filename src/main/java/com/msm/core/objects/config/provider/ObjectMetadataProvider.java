package com.msm.core.objects.config.provider;

import com.msm.core.dynamicquery.context.ObjectMetadataContextProvider;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.service.GenericObjectMetadataService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ObjectMetadataProvider implements ObjectMetadataContextProvider {

    private final GenericObjectMetadataService genericObjectMetadataService;

    @Override
    public Optional<ObjectMetadata> getObjectMetadata(String name) {
        return genericObjectMetadataService.getObjectMetadata(name);
    }
}
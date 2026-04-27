package com.msm.core.objects.generic.converter;


import com.msm.core.commons.ObjectValueConverter;
import com.msm.core.commons.Utils;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.service.GenericObjectMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GenericObjectConverter implements ObjectValueConverter<Map<String, Object>> {

    private final EntityClassFactory entityClassFactory;
    private final GenericObjectMetadataService genericObjectMetadataService;

    @Override
    public <T> T convert(String objectName, Map<String, Object> from, Object optionalParam) {
        Class<?> cl = entityClassFactory.getEntityType(objectName).getJavaType();
        Optional<ObjectMetadata> objectAttribute = genericObjectMetadataService.getObjectAttribute(objectName);
        if(objectAttribute.isEmpty()) {
            log.warn("No such object attribute with name {}", objectName);
        }
        return (T) Utils.O.toObject(from, cl);
    }
}

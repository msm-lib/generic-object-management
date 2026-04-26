package com.msm.core.objects.generic.converter;


import com.msm.core.commons.ObjectValueConverter;
import com.msm.core.commons.Utils;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.service.GenericAttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component(value = "genericObjectConverter")
@RequiredArgsConstructor
public class GenericObjectConverter implements ObjectValueConverter<Map<String, Object>> {

    private final EntityClassFactory entityClassFactory;
    private final GenericAttributeService genericAttributeService;

    @Override
    public <T> T convert(String objectName, Map<String, Object> from, Object optionalParam) {
        Class<?> cl = entityClassFactory.getEntityType(objectName).getJavaType();
        Optional<ObjectMetadata> objectAttribute = genericAttributeService.getObjectAttribute(objectName);
        if(objectAttribute.isEmpty()) {
            log.warn("No such object attribute with name {}", objectName);
        }
        return (T) Utils.O.toObject(from, cl);
    }
}

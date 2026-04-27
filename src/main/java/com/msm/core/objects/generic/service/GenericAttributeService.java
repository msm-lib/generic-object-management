package com.msm.core.objects.generic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.ObjectConstants;
import com.msm.core.validate.ObjectAttributeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
//@Service
public class GenericAttributeService {

    public Optional<ObjectMetadata> getObjectAttribute(String objectName) {
        String objectKey = Utils.STR.lowCase(objectName);
        Optional<ObjectMetadata> objectAttribute = ObjectAttributeFactory.get(objectKey);
        if(objectAttribute.isPresent()) {
            return objectAttribute;
        }
        try {
            ClassPathResource attributeResource = new ClassPathResource(Utils.STR.format(ObjectConstants.ATTRIBUTE_PATH_TEMPLATE, objectKey));
            if(!attributeResource.exists()) {
                log.warn("Attribute resource {} not found", objectKey);
                return Optional.empty();
            }
            ObjectMetadata objectMetadata = new ObjectMapper().readValue(attributeResource.getInputStream(), new TypeReference<>() {});
            ObjectMetadataFactory.registerObjectMetadata(objectMetadata);
            ObjectAttributeFactory.register(objectKey,  objectMetadata);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return ObjectAttributeFactory.get(objectKey);
    }
}

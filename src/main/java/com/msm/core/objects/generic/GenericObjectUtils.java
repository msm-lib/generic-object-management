package com.msm.core.objects.generic;

import com.fasterxml.jackson.databind.JavaType;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.entity.ReferenceFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({"unchecked"})
public class GenericObjectUtils {
    private GenericObjectUtils() {}

    public static <X> void updateRef(X oldData, Map<String, Object> newData) {
        Map<String, Object> objectMap = (Map<String, Object>) Utils.O.getProperty(oldData, ObjectConstants.CUSTOM_VALUE_FIELD_NAME);
        if(Objects.nonNull(objectMap) && oldData instanceof ReferenceFields referenceFields) {
            Utils.CL.emptyIfNull(referenceFields.referenceFields()).forEach(referenceField -> {
                String fieldRef = referenceFields.referenceField(referenceField);
                Object objectRef = newData.get(fieldRef);
                if(Objects.nonNull(objectRef)) {
                    objectMap.put(fieldRef, objectRef);
                }
            });
        }
    }

    public static void updateRef(ObjectMetadata objectMetadata, Map<String, Object> newData) {
        Attribute customFieldAttribute = objectMetadata.getCustomFieldAttribute();
        if (customFieldAttribute != null) {
           JavaType customFieldAttributeJavaType =  customFieldAttribute.getJavaType();
           if(customFieldAttributeJavaType != null && customFieldAttributeJavaType.isMapLikeType()) {
               Map<String, Object> objectMap = (Map<String, Object>) newData.getOrDefault(customFieldAttribute.getFieldName(), new HashMap<>());
               objectMetadata.getAttributes().forEach(attribute -> {
                   if(attribute.getAttributeRef() != null) {
                       Object valueRef = newData.get(attribute.getAttributeRef());
                       if(Objects.nonNull(valueRef)) {
                           objectMap.put(attribute.getAttributeRef(), valueRef);
                       }
                   }
               });
               newData.put(customFieldAttribute.getFieldName(), objectMap);
           }
       }
    }

    public static void mapping(String targetKey, Object sourceKey, Map<String, Object> source, Map<String, Object> target) {
        Object value;
        if(sourceKey instanceof Function mapFunction) {
            value = mapFunction.apply(source);
        } else {
            value = source.get(String.valueOf(sourceKey));
        }
        if(Objects.nonNull(value)) {
            target.put(targetKey, value);
        }
    }
}

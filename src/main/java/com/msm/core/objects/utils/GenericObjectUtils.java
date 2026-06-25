package com.msm.core.objects.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.entity.ReferenceFields;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
                           objectMap.put(attribute.getAttributeRef().getFieldName(), valueRef);
                       }
                   }
               });
               newData.put(customFieldAttribute.getFieldName(), objectMap);
           }
       }
    }

    public static void processUnwrappedCustomValues(Object object) {
        if(Map.class.isAssignableFrom(object.getClass())) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            Optional<Map.Entry<String, Object>> objectCustomEntry =  objectMap
                    .entrySet()
                    .stream()
                    .filter(entry -> ObjectConstants.CUSTOM_VALUE_FIELD_NAME.equals(entry.getKey()))
                    .findAny();
            if(objectCustomEntry.isPresent()) {
                Map<String, Object> customFieldValues = (Map<String, Object>) objectCustomEntry.get().getValue();
                if(Objects.nonNull(customFieldValues)) {
                    objectMap.putAll(customFieldValues);
                    objectMap.remove(objectCustomEntry.get().getKey());
                }
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

    public static String normalizeHttpsUrl(String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (uri.getScheme() == null) {
            return "https://" + baseUrl;
        }
        return baseUrl;
    }

    public static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        url = url.trim();

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (!url.matches("^(?i)https?://.*")) {
            url = "https://" + url;
        }

        return url;
    }
}

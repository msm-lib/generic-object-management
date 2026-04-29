package com.msm.core.objects.generic.converter;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCustomValueMappingStrategy implements CustomValueMappingStrategy {
    @Override
    public String supportObjectType() {
        return DEFAULT_OBJECT_TYPE;
    }

    @Override
    public void from(ObjectMetadata meta, Map<String, Object> databaseObject) {
        if (databaseObject != null) {
            Attribute customAttr = meta.getAttributeByName(Constants.CUSTOM_VALUE_NAME);
            if(customAttr != null) {
                Object customValuesObject = databaseObject.get(customAttr.getFieldName());
                if(customValuesObject != null) {
                    Map<String, Object> customValuesMap = Utils.O.toMap(customValuesObject);
                    databaseObject.putAll(customValuesMap);
                    //remove customValues
                    databaseObject.remove(customAttr.getFieldName());
                }
            }
        }
    }

    @Override
    public void to(ObjectMetadata meta, Map<String, Object> userObject) {
        if (userObject != null) {
            Map<String, Object> customValues = getCustomValues(meta, userObject);
            // inject field customValues to payload
            Attribute customAttr = meta.getAttributeByName(Constants.CUSTOM_VALUE_NAME);
            if (customAttr != null) {
                userObject.put(Constants.CUSTOM_VALUE_NAME, customValues);
            }
        }
    }

    private static Map<String, Object> getCustomValues(ObjectMetadata meta, Map<String, Object> userObject) {
        Map<String, Object> customValues = new HashMap<>();
        //Get all attribute ref
        List<Attribute> attributes =  meta.getAttributeRefs();
        attributes.forEach(attribute -> {
            String attributeRef = attribute.getAttributeRef();
            Object attributeRefValue = userObject.get(attributeRef);
            if (attributeRefValue != null) {
                customValues.put(attributeRef, attributeRefValue);
                //maybe remove ref
            }
        });
        return customValues;
    }
}

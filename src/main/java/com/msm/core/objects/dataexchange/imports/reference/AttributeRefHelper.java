package com.msm.core.objects.dataexchange.imports.reference;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.objects.config.ObjectImportRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributeRefHelper {
//    private static final List<String> DEFAULT_RETURN_FIELDS = List.of(
//            "id",
//            "code",
//            "name"
//    );
//    public static List<String> getOrDefaultReturnFields(AttributeRef attributeRef){
//        if(Utils.CL.isEmpty(attributeRef.getAttributeRefs())) {
//            return DEFAULT_RETURN_FIELDS;
//        }
//        return Utils.CL.newArrayList(attributeRef.getAttributeRefs());
//    }

    public static Set<String> getCodes(Attribute attribute, List<Map<String, Object>> items) {
        return Utils.CL.emptyIfNull(Utils.D.toListByKey(items, attribute.getFieldName()))
                .stream()
                .map(Object::toString)
                .filter(s -> Utils.STR.isNotBlank(s) && !Utils.STR.isEmpty(s))
                .collect(Collectors.toSet());
    }

    public static Set<String> getLookupValues(String sourceAttributeName, List<Map<String, Object>> items) {
        return Utils.CL.emptyIfNull(Utils.D.toListByKey(items, sourceAttributeName))
                .stream()
                .map(Object::toString)
                .filter(s -> Utils.STR.isNotBlank(s) && !Utils.STR.isEmpty(s))
                .collect(Collectors.toSet());
    }

//    public static Map<String, Set<String>> getLookupValueMap(Attribute sourceAttr, List<AttributeLookup> attributeLookups, List<Map<String, Object>> items) {
//        Map<String, Set<String>> map = new HashMap<>();
//        attributeLookups.forEach(attributeLookup -> {
//            if(Utils.CL.isEmpty(attributeLookup.defaultValues())) {
//                map.put(attributeLookup.attributeName(), getLookupValues(sourceAttr.getFieldName(), items));
//            } else {
//                map.put(attributeLookup.attributeName(), attributeLookup.defaultValues());
//            }
//        });
//        return map;
//    }

    public static Map<String, Set<String>> getLookupValueMap(
            Attribute sourceAttr,
            List<ObjectImportRegistry.LookupValuesConfig> attributeLookups,
            List<Map<String, Object>> items
    ) {
        Map<String, Set<String>> map = new HashMap<>();
        attributeLookups.forEach(attributeLookup -> {
            if(Utils.CL.isEmpty(attributeLookup.getDefaultValues())) {
                map.put(attributeLookup.getAttributeName(), getLookupValues(sourceAttr.getFieldName(), items));
            } else {
                map.put(attributeLookup.getAttributeName(), attributeLookup.getDefaultValues());
            }
        });
        return map;
    }

    public static boolean hasRef(Attribute attribute) {
        return Objects.nonNull(attribute.getAttributeRef()) && Utils.STR.isNotBlank(attribute.getAttributeRef().getFieldName());
    }

    public static void retainAllRefData(Set<String> refNames, List<Map<String, Object>> objectList) {
        objectList.forEach(objectValue -> {
            objectValue.keySet().retainAll(refNames);
        });
    }
}

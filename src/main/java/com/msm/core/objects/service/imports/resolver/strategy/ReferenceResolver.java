package com.msm.core.objects.service.imports.resolver.strategy;


import com.msm.core.metadata.Attribute;

import java.util.List;
import java.util.Map;

public interface ReferenceResolver {
    String object();
    String attribute();
    default List<String> returnFields() {
        return List.of(
                "id",
                "code",
                "name"
        );
    }
    Map<String, Map<String, Map<String, Object>>> resolve(String objectName, Attribute attribute, List<Map<String, Object>> items);
}

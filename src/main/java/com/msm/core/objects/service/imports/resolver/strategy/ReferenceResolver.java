package com.msm.core.objects.service.imports.resolver.strategy;


import com.msm.core.metadata.Attribute;
import com.msm.core.strategy.TypedStrategy;

import java.util.List;
import java.util.Map;

public interface ReferenceResolver extends TypedStrategy<String> {
    List<String> DEFAULT_RETURN_FIELDS = List.of(
            "id",
            "code",
            "name"
    );

    String sourceObject();
    String sourceAttribute();
    String targetObject();

    Map<String, Map<String, Map<String, Object>>> resolve(String sourceObjectName, Attribute attribute, List<Map<String, Object>> items);
}

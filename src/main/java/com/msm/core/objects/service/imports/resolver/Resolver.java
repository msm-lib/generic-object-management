package com.msm.core.objects.service.imports.resolver;

import com.msm.core.metadata.Attribute;

import java.util.List;
import java.util.Map;

public interface Resolver {
    Map<String, Map<String, Map<String, Object>>> resolve(String objectName, Attribute attribute, List<Map<String, Object>> items);
}

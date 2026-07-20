package com.msm.core.objects.service.imports.resolver;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.service.imports.resolver.strategy.ReferenceResolver;
import com.msm.core.strategy.StrategyResolver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ObjectResolver implements Resolver {

    private final StrategyResolver<String, ReferenceResolver> objectRefResolverFactory;

    @Override
    public Map<String, Map<String, Map<String, Object>>> resolve(String sourceObjectName, Attribute attribute, List<Map<String, Object>> items) {
        ReferenceResolver referenceResolver = objectRefResolverFactory.resolve(Utils.STR.format(ObjectConstants.OBJECT_ATTRIBUTE_REF_TEMPLATE, sourceObjectName, attribute.getAttributeRef().getObjectRef(), attribute.getFieldName()));
        return referenceResolver.resolve(sourceObjectName, attribute, items);
    }

}

package com.msm.core.objects.service.imports.resolver.strategy;


import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class ObjectRefResolverFactory extends AbstractStrategyRegistry<String, ReferenceResolver> {
    public ObjectRefResolverFactory(List<ReferenceResolver> referenceResolvers, ReferenceResolver defaultReferenceResolver) {
        super(referenceResolvers, defaultReferenceResolver);
    }
}

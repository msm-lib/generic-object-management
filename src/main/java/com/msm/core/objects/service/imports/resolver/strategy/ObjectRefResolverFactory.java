package com.msm.core.objects.service.imports.resolver.strategy;

import com.msm.core.commons.Utils;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class ObjectRefResolverFactory extends AbstractStrategyRegistry<ReferenceResolver> {
    public ObjectRefResolverFactory(List<ReferenceResolver> referenceResolvers, ReferenceResolver defaultReferenceResolver) {
        super(referenceResolvers, defaultReferenceResolver);
    }

    @Override
    protected String supportObjectType(ReferenceResolver referenceResolver) {
        return Utils.STR.format(ObjectConstants.OBJECT_ATTRIBUTE_REF_TEMPLATE, referenceResolver.object(), referenceResolver.attribute());
    }
}

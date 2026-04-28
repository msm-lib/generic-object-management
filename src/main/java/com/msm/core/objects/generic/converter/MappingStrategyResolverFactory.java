package com.msm.core.objects.generic.converter;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class MappingStrategyResolverFactory extends AbstractStrategyRegistry<ObjectMappingStrategy> {
    public MappingStrategyResolverFactory(List<ObjectMappingStrategy> strategies, ObjectMappingStrategy defaultStrategy) {
        super(strategies, defaultStrategy);
    }

    @Override
    protected String supportObjectType(ObjectMappingStrategy objectMappingStrategy) {
        return objectMappingStrategy.supportObjectType();
    }
}

package com.msm.core.objects.converter;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class MappingStrategyResolverFactory extends AbstractStrategyRegistry<CustomValueMappingStrategy> {
    public MappingStrategyResolverFactory(List<CustomValueMappingStrategy> strategies, CustomValueMappingStrategy defaultStrategy) {
        super(strategies, defaultStrategy);
    }

    @Override
    protected String supportObjectType(CustomValueMappingStrategy customValueMappingStrategy) {
        return customValueMappingStrategy.supportObjectType();
    }
}

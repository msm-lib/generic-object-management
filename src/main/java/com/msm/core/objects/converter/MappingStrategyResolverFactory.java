package com.msm.core.objects.converter;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class MappingStrategyResolverFactory extends AbstractStrategyRegistry<String, CustomValueMappingStrategy> {
    public MappingStrategyResolverFactory(List<CustomValueMappingStrategy> strategies, CustomValueMappingStrategy defaultStrategy) {
        super(strategies, defaultStrategy);
    }
}

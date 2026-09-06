package com.msm.core.objects.security;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class SecurityFieldResolverFactory extends AbstractStrategyRegistry<String, SecurityFieldResolver> {
    public SecurityFieldResolverFactory(List<SecurityFieldResolver> strategies, SecurityFieldResolver defaultStrategy) {
        super(strategies, defaultStrategy);
    }
}


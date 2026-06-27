package com.msm.core.objects.security;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class SecurityFieldResolverFactory extends AbstractStrategyRegistry<SecurityFieldResolver> {
    public SecurityFieldResolverFactory(List<SecurityFieldResolver> strategies, SecurityFieldResolver defaultStrategy) {
        super(strategies, defaultStrategy);
    }

    @Override
    protected String supportObjectType(SecurityFieldResolver strategy) {
        return strategy.supportObjectType();
    }
}


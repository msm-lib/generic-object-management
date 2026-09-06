package com.msm.core.objects.audit;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class AuditStrategyResolverFactory extends AbstractStrategyRegistry<String, AuditStrategy> {

    public AuditStrategyResolverFactory(List<AuditStrategy> strategies, AuditStrategy defaultStrategy) {
        super(strategies, defaultStrategy);
    }
}
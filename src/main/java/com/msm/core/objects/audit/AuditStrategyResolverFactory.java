package com.msm.core.objects.audit;

import com.msm.core.strategy.AbstractStrategyRegistry;

import java.util.List;

public class AuditStrategyResolverFactory extends AbstractStrategyRegistry<AuditStrategy> {

    public AuditStrategyResolverFactory(List<AuditStrategy> strategies, AuditStrategy defaultStrategy) {
        super(strategies, defaultStrategy);
    }

    @Override
    protected String supportObjectType(AuditStrategy auditStrategy) {
        return auditStrategy.supportObjectType();
    }

}
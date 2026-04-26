package com.msm.core.objects.generic.rules;

import com.msm.core.objects.config.genric.DynamicRulesFactory;
import lombok.RequiredArgsConstructor;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.RuleListener;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenericObjectRulesService {
    private final DynamicRulesFactory rulesFactory;

    public <T> RuleResult executeRule(String ruleGroupName, T request) {

        RuleContext<T> context = new RuleContext<>(ruleGroupName, request, RuleResult.create());
        Rules rules = rulesFactory.getRules(ruleGroupName);
        Facts facts = new Facts();
        RuleContextUtils.createContext(facts, context);
        DefaultRulesEngine engine = new DefaultRulesEngine();
        engine.fire(rules, facts);

        return context.getRuleResult();
    }

    public <T> RuleResult executeRule(String groupName, T request, RuleListener ruleListener) {

        RuleContext<T> context = new RuleContext<>(groupName, request, RuleResult.create());
        Rules rules = rulesFactory.getRules(groupName);
        Facts facts = new Facts();
        RuleContextUtils.createContext(facts, context);
        DefaultRulesEngine engine = new DefaultRulesEngine();
        engine.registerRuleListener(ruleListener);
        engine.fire(rules, facts);

        return context.getRuleResult();
    }
}

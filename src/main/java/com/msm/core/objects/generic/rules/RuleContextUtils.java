package com.msm.core.objects.generic.rules;

import org.jeasy.rules.api.Facts;

public final class RuleContextUtils {
    private static final String CONTEXT_NAME = "context";

    private RuleContextUtils() {}

    public static <T> com.msm.core.objects.generic.rules.RuleContext<T> getRuleContext(Facts facts) {
        return facts.get(CONTEXT_NAME);
    }

    public static <T> void createContext(Facts facts, RuleContext<T> context) {
        facts.put(CONTEXT_NAME, context);
    }
}

package com.msm.core.objects.config.genric;

import com.msm.core.objects.generic.annotation.ActionRules;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRulesFactory {

    private final ApplicationContext applicationContext;
    private final Map<String, Rules> cacheGroups = new ConcurrentHashMap<>();

    @PostConstruct
    void initRules() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ActionRules.class);
        beans.forEach((name, container) -> {
            ActionRules anno = AnnotationUtils.findAnnotation(container.getClass(), ActionRules.class);
            if(anno == null) {
                log.warn("Could not find @ActionRules on bean: {}", name);
                return;
            }
            Rules rulesInContainer = new Rules();
            Method[] methods = container.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getReturnType().equals(Rule.class)) {
                    try {
                        Rule rule = (Rule) method.invoke(container);
                        rulesInContainer.register(rule);
                    } catch (Exception e) {
                        log.error("Failed to register rule from method: {}", method.getName(), e);
                    }
                }
            }
            String groupName = anno.value();
            Rules existingRules = cacheGroups.computeIfAbsent(groupName, k -> new Rules());
            rulesInContainer.forEach(existingRules::register);
        });
    }

    public Rules getRules(String groupName) {
        return cacheGroups.getOrDefault(groupName, new Rules());
    }

}


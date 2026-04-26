package com.msm.core.objects.config.genric;

import com.msm.core.hook.*;
import com.msm.core.hook.anontation.Handler;
import com.msm.core.hook.anontation.Hook;
import com.msm.core.hook.common.TransactionHook;
import com.msm.core.hook.context.AnnotationConfig;
import com.msm.core.hook.context.KeyDimensionResolver;
import com.msm.core.objects.exception.UnableCreateInstanceException;
import com.msm.core.validate.attr.ValueValidationHandler;
import com.msm.core.validate.attr.ValueValidationHandlerFactory;
import com.msm.core.validate.attr.rules.AttributeSimpleRule;
import com.msm.core.validate.attr.rules.AttributeSimpleRuleFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"unchecked"})
@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectConfigInitializer implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    private final ObjectProvider<ValueValidationHandler> valueValidationHandlers;
    private final ObjectProvider<AttributeSimpleRule> attributeSimpleRules;
    private final ObjectProvider<TransactionHook> transactionHooks;

    @Override
    public void afterSingletonsInstantiated() {

        Map<String, List<HookDefinitionExecutor>> hookMap = new HashMap<>();

        // Scan bean with annotation
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (Object bean : beans.values()) {
            Class<?> targetClass = getTargetClass(bean);
            if (!hasAnyCandidate(targetClass)) continue;
            ReflectionUtils.doWithMethods(
                    targetClass,
                    method -> processMethod(bean, method, hookMap),
                    this::isCandidateMethod
            );
        }

        // sort + register hook
        hookMap.forEach((key, list) -> {
            list.sort(Comparator.comparingInt(HookDefinitionExecutor::getOrder));
            HookDefinitionHandlerFactory.register(key, List.copyOf(list));
        });

        valueValidationHandlers.forEach(ValueValidationHandlerFactory::register);
        attributeSimpleRules.forEach(AttributeSimpleRuleFactory::register);

        TransactionUtils.setHook(transactionHooks.getIfAvailable());

        log.info("ObjectConfigInitializer initialized successfully");
    }

    private void processMethod(Object bean, Method method, Map<String, List<HookDefinitionExecutor>> hookMap) {

        Hook typeHook = AnnotatedElementUtils.findMergedAnnotation(method, Hook.class);
        if (typeHook != null) {
            log.info("Found HookDefinitionExecutor for method {} in bean {}", method.toGenericString(), bean.getClass().getName());
            registerHook(bean, method, hookMap);
        }

        Handler handler = AnnotatedElementUtils.findMergedAnnotation(method, Handler.class);
        if (handler != null) {
            registerHandler(bean, method, handler);
        }
    }

    private void registerHook(Object bean, Method method, Map<String, List<HookDefinitionExecutor>> hookMap) {

        AnnotationConfig hookConfig = AnnotationConfig.fromHookMethod(method);
        String hookKey = KeyDimensionResolver.resolve(hookConfig);
        hookMap.computeIfAbsent(hookKey, k -> new ArrayList<>()).add(HookDefinitionExecutor.create(
                bean,
                method,
                hookConfig.order(),
                resolveObject(hookConfig.condition()),
                hookConfig.stopOnError()
        ));
    }

    private void registerHandler(Object bean, Method method, Handler handler) {

        AnnotationConfig hookConfig = AnnotationConfig.fromHandlerMethod(method);
        String key = KeyDimensionResolver.resolveHandler(hookConfig);
        ActionHandlerFactory.register(key, ActionDefinitionExecutor.create(
                bean,
                method,
                handler,
                resolveObject(handler.condition()))
        );
    }

    private boolean hasAnyCandidate(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (isCandidateMethod(method)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCandidateMethod(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, Hook.class) != null
                || AnnotatedElementUtils.findMergedAnnotation(method, Handler.class) != null;
    }

    private Class<?> getTargetClass(Object bean) {
        return AopUtils.getTargetClass(bean);
    }

    private <X> X resolveObject(Class<X> clazz) {
        if (clazz == null) return null;

        try {
            return applicationContext.getBean(clazz);
        } catch (Exception e) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new UnableCreateInstanceException("Cannot create instance: " + clazz, ex);
            }
        }
    }
}
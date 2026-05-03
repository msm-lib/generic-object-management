package com.msm.core.objects.config;

import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.context.ObjectMetadataContextHolder;
import com.msm.core.hook.ActionDefinitionExecutor;
import com.msm.core.hook.ActionHandlerFactory;
import com.msm.core.hook.HookDefinitionExecutor;
import com.msm.core.hook.HookDefinitionHandlerFactory;
import com.msm.core.hook.TransactionUtils;
import com.msm.core.hook.anontation.Handler;
import com.msm.core.hook.anontation.Hook;
import com.msm.core.hook.common.TransactionHook;
import com.msm.core.hook.context.AnnotationUtility;
import com.msm.core.hook.context.KeyDimensionResolver;
import com.msm.core.objects.config.provider.ObjectMetadataProvider;
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
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unchecked"})
@Slf4j
//@Component
@RequiredArgsConstructor
public class ObjectBeanConfigInitializing implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    private final ObjectProvider<ValueValidationHandler> valueValidationHandlers;
    private final ObjectProvider<AttributeSimpleRule> attributeSimpleRules;
    private final ObjectProvider<TransactionHook> transactionHooks;
    private final ObjectProvider<ObjectMetadataProvider> objectMetadataContexts;


    @Override
    public void afterSingletonsInstantiated() {

        Map<String, List<HookDefinitionExecutor>> hookMap = new HashMap<>();

        // Scan bean with annotation
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
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
            HookDefinitionHandlerFactory.register(key, list);
        });

        valueValidationHandlers.forEach(ValueValidationHandlerFactory::register);
        attributeSimpleRules.forEach(AttributeSimpleRuleFactory::register);

        TransactionUtils.setHook(transactionHooks.getIfAvailable());
        ObjectMetadataContextHolder.setObjectMetadataContextProvider(objectMetadataContexts.getIfAvailable());
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

//        ActionType type = AnnotationConfig.getActionType(method);
//        ActionKeyStrategyResolver<?,?,?> actionKeyStrategyResolver = resolveObject(type.contextKeyResolver());
//        actionKeyStrategyResolver.resolve()
//        String key = AnnotationConfig.fromHookMethod(method, (ActionKeyStrategyResolver<?, ?, AnnotatedElement>) actionKeyStrategyResolver);
//        System.out.println(key);
//        Map<String, Object> stringObjectMap = AnnotationUtility.getMergedAttributes(method, ActionType.class);
//        System.out.println(stringObjectMap);
//        ActionKeyStrategyResolver<?, String, AnnotatedElement> actionKeyStrategyResolver0 = (ActionKeyStrategyResolver<?, String, AnnotatedElement>) actionKeyStrategyResolver;
//        AnnotationConfig hookConfig = AnnotationConfig.fromHookMethod(method);
//        String hookKey = KeyDimensionResolver.resolve(hookConfig);
        AnnotationUtility annotationUtility = AnnotationUtility.getAnnotationConfig(method);
        String hookKey = KeyDimensionResolver.resolveHookKey(annotationUtility);
        hookMap.computeIfAbsent(hookKey, k -> Utils.CL.newLinkedList()).add(HookDefinitionExecutor.create(
                bean,
                method,
                annotationUtility.getOrder(),
                resolveObject(annotationUtility.getCondition()),
                annotationUtility.isStopOnError()
        ));
    }

    private void registerHandler(Object bean, Method method, Handler handler) {
//        ActionType type = AnnotationConfig.getActionType(method);
//        ActionKeyStrategyResolver<?,?,?> actionKeyStrategyResolver = resolveObject(type.contextKeyResolver());
//        ActionKeyStrategyResolver<?, String, AnnotatedElement> actionKeyStrategyResolver0 = (ActionKeyStrategyResolver<?, String, AnnotatedElement>) actionKeyStrategyResolver;

//        AnnotationConfig hookConfig = AnnotationConfig.fromHandlerMethod(method);
//        String name = method.getName();
//        System.out.println(name);
//        String key = KeyDimensionResolver.resolveHandler(hookConfig);
        AnnotationUtility annotationUtility = AnnotationUtility.getAnnotationConfig(method);
        String handlerKey = KeyDimensionResolver.resolveHandlerKey(annotationUtility);
        System.out.println(handlerKey);
        ActionHandlerFactory.register(handlerKey, ActionDefinitionExecutor.create(
                bean,
                method,
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
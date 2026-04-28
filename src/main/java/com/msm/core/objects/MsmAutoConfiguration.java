package com.msm.core.objects;

import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.DefaultPredicateFactory;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.hook.DefaultActionExecutor;
import com.msm.core.hook.DefaultAsyncExecutor;
import com.msm.core.hook.DefaultHookEngine;
import com.msm.core.hook.common.ActionExecutor;
import com.msm.core.hook.common.HookEngine;
import com.msm.core.hook.common.TransactionHook;
import com.msm.core.objects.config.DynamicRulesFactory;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.config.ObjectBeanConfigInitializing;
import com.msm.core.objects.config.provider.ObjectMetadataProvider;
import com.msm.core.objects.generic.audit.AuditStrategy;
import com.msm.core.objects.generic.audit.AuditStrategyResolverFactory;
import com.msm.core.objects.generic.audit.DefaultAuditStrategy;
import com.msm.core.objects.generic.controller.GenericObjectController;
import com.msm.core.objects.generic.converter.DefaultObjectMappingStrategy;
import com.msm.core.objects.generic.converter.MappingStrategyResolverFactory;
import com.msm.core.objects.generic.converter.ObjectMappingStrategy;
import com.msm.core.objects.generic.handler.GenericObjectHandler;
import com.msm.core.objects.generic.hook.GenericHookEvent;
import com.msm.core.objects.generic.repository.DefaultRepositoryFactory;
import com.msm.core.objects.generic.repository.RepositoryFactory;
import com.msm.core.objects.generic.rules.GenericObjectRulesService;
import com.msm.core.objects.generic.service.DefaultSoftDeleteFilter;
import com.msm.core.objects.generic.service.GenericAttributeService;
import com.msm.core.objects.generic.service.GenericObjectMetadataService;
import com.msm.core.objects.generic.service.GenericObjectService;
import com.msm.core.objects.generic.service.PreprocessCustomFieldValueService;
import com.msm.core.objects.generic.transaction.ObjectTransactionHook;
import com.msm.core.strategy.StrategyResolver;
import com.msm.core.validate.attr.ValueValidationHandler;
import com.msm.core.validate.attr.rules.AttributeSimpleRule;
import com.msm.core.validate.validation.AttributeTypeValidator;
import com.msm.core.validate.validation.AttributeValidator;
import com.msm.core.validate.validation.DefaultAttributeValidator;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

@AutoConfiguration
//@ConditionalOnClass(DSLContext.class) // 🔥 chỉ enable nếu có jOOQ
@EnableConfigurationProperties(GenericObjectConfigProperties.class)
public class MsmAutoConfiguration {

//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Bean(name = "hookTaskExecutor")
//    public Executor hookTaskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);
//        executor.setMaxPoolSize(20);
//        executor.setThreadNamePrefix("HookTaskExecutor-");
//        executor.initialize();
//        return executor;
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(DSLContext.class)
//    public DynamicQueryService dynamicQueryService(DSLContext dslContext) {
//        return new DynamicQueryService(dslContext);
//    }
//
//    @Bean
//    public JPAQueryFactory jpaQueryFactory() {
//        return new JPAQueryFactory(entityManager);
//    }
//
//    @Bean
//    public EntityClassFactory entityClassFactory() {
//        return new EntityClassFactory(entityManager);
//    }
//
//    @Bean
//    public AdvancedFilterService advancedFilterService(JPAQueryFactory queryFactory, EntityClassFactory entityClassFactory) {
//        return new AdvancedFilterService(queryFactory, new DefaultPredicateFactory(), entityClassFactory);
//    }
//
//    @Bean
//    public HookEngine hookEngine(Executor hookTaskExecutor) {
//        return new DefaultHookEngine(new DefaultAsyncExecutor(hookTaskExecutor));
//    }
//
//    @Bean
//    public ActionExecutor actionExecutor(HookEngine hookEngine) {
//        return new DefaultActionExecutor(hookEngine);
//    }
//
//    @Bean(name = "attributeTypeValidator")
//    public AttributeValidator attributeDataTypeValidator() {
//        return new AttributeTypeValidator();
//    }
//
//    @Bean(name = "defaultAttributeValidator")
//    public AttributeValidator attributeValidator(AttributeValidator attributeTypeValidator) {
//        return new DefaultAttributeValidator(attributeTypeValidator);
//    }



    // ========= EXECUTOR =========
    @Bean(name = "hookTaskExecutor")
    @ConditionalOnMissingBean(name = "hookTaskExecutor")
    public Executor hookTaskExecutor(GenericObjectConfigProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getExecutor().getCore());
        executor.setMaxPoolSize(props.getExecutor().getMax());
        executor.setThreadNamePrefix("HookTaskExecutor-");
        executor.initialize();
        return executor;
    }

    // ========= JOOQ =========
    @Bean
//    @ConditionalOnBean(DSLContext.class)
    @ConditionalOnMissingBean
    public DynamicQueryService dynamicQueryService(DSLContext dslContext) {
        return new DynamicQueryService(dslContext);
    }

    // ========= JPA =========
    @Bean
//    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
//    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean
    public EntityClassFactory entityClassFactory(EntityManager entityManager) {
        return new EntityClassFactory(entityManager);
    }

    @Bean
//    @ConditionalOnBean({JPAQueryFactory.class, EntityClassFactory.class})
    @ConditionalOnMissingBean
    public AdvancedFilterService advancedFilterService(
            JPAQueryFactory queryFactory,
            EntityClassFactory entityClassFactory
    ) {
        return new AdvancedFilterService(
                queryFactory,
                new DefaultPredicateFactory(),
                entityClassFactory
        );
    }

    // ========= HOOK =========
    @Bean
    @ConditionalOnMissingBean
    public HookEngine hookEngine(Executor hookTaskExecutor) {
        return new DefaultHookEngine(new DefaultAsyncExecutor(hookTaskExecutor));
    }

    @Bean
    @ConditionalOnMissingBean
    public ActionExecutor actionExecutor(HookEngine hookEngine) {
        return new DefaultActionExecutor(hookEngine);
    }

    // ========= VALIDATOR =========
    @Bean(name = "attributeTypeValidator")
    @ConditionalOnMissingBean(name = "attributeTypeValidator")
    public AttributeValidator attributeTypeValidator() {
        return new AttributeTypeValidator();
    }

    @Bean(name = "defaultAttributeValidator")
    @ConditionalOnMissingBean(name = "defaultAttributeValidator")
    public AttributeValidator defaultAttributeValidator(AttributeValidator attributeTypeValidator) {
        return new DefaultAttributeValidator(attributeTypeValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicRulesFactory dynamicRulesFactory(ApplicationContext applicationContext) {
        return new DynamicRulesFactory(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectBeanConfigInitializing objectConfigInitializer(ApplicationContext applicationContext,
                                                                ObjectProvider<ValueValidationHandler> valueValidationHandlers,
                                                                ObjectProvider<AttributeSimpleRule> attributeSimpleRules,
                                                                ObjectProvider<TransactionHook> transactionHooks,
                                                                ObjectProvider<ObjectMetadataProvider> objectMetadataContexts) {

        return new ObjectBeanConfigInitializing(
                applicationContext,
                valueValidationHandlers,
                attributeSimpleRules,
                transactionHooks,
                objectMetadataContexts
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultSoftDeleteFilter defaultSoftDeleteFilter(EntityClassFactory entityClassFactory) {
        return new DefaultSoftDeleteFilter(entityClassFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericAttributeService genericAttributeService() {
        return new GenericAttributeService();
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericHookEvent genericHookEvent(
            @Qualifier("defaultAttributeValidator") AttributeValidator defaultAttributeValidator,
            GenericObjectMetadataService genericObjectMetadataService) {
        return new GenericHookEvent(defaultAttributeValidator, genericObjectMetadataService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RepositoryFactory repositoryFactory(List<JpaRepository<?, ?>> repositories, ListableBeanFactory beanFactory) {
        return new DefaultRepositoryFactory(repositories, beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericObjectRulesService genericObjectRulesService(DynamicRulesFactory rulesFactory) {
        return new GenericObjectRulesService(rulesFactory);
    }

    @Bean
    public GenericObjectMetadataService genericObjectMetadataService(DSLContext dslContext) {
        return new GenericObjectMetadataService(dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditStrategy defaultAuditStrategy() {
        return new DefaultAuditStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditStrategyResolverFactory auditStrategyRegistry(List<AuditStrategy> auditStrategies, AuditStrategy defaultAuditStrategy) {
        return new AuditStrategyResolverFactory(auditStrategies, defaultAuditStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMappingStrategy defaultObjectMappingStrategy() {
        return new DefaultObjectMappingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingStrategyResolverFactory objectMappingStrategyFactory(List<ObjectMappingStrategy> objectMappingStrategies, ObjectMappingStrategy defaultObjectMappingStrategy) {
        return new MappingStrategyResolverFactory(objectMappingStrategies, defaultObjectMappingStrategy);
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public MappingStrategyResolverFactory0 objectMappingStrategyFactory0(List<ObjectMappingStrategy> objectMappingStrategies0, ObjectMappingStrategy defaultObjectMappingStrategy0) {
//        return new MappingStrategyResolverFactory0(objectMappingStrategies0, defaultObjectMappingStrategy0);
//    }



    @Bean
    @ConditionalOnMissingBean
    public GenericObjectHandler genericObjectExecutor(
            DynamicQueryService dynamicQueryService,
            GenericObjectMetadataService genericObjectMetadataService,
            StrategyResolver<AuditStrategy> auditStrategyFactory,
            StrategyResolver<ObjectMappingStrategy> objectMappingStrategyFactory
    ) {
        return new GenericObjectHandler(
                dynamicQueryService,
                genericObjectMetadataService,
                auditStrategyFactory,
                objectMappingStrategyFactory
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public PreprocessCustomFieldValueService preprocessCustomFieldValueService() {
        return new PreprocessCustomFieldValueService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectTransactionHook objectTransactionHook() {
        return new ObjectTransactionHook();
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericObjectService genericObjectService(
            ActionExecutor actionExecutor
    ) {
        return new GenericObjectService(actionExecutor);
    }

    @Bean
    public GenericObjectController genericObjectController(GenericObjectService service, GenericObjectMetadataService genericObjectMetadataService) {
        return new GenericObjectController(service, genericObjectMetadataService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMetadataProvider objectMetadataProvider(GenericObjectMetadataService genericObjectMetadataService) {
        return new ObjectMetadataProvider(genericObjectMetadataService);
    }
}

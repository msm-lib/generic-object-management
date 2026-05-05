package com.msm.core.objects;

import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.action.executor.DefaultActionExecutor;
import com.msm.core.action.executor.DefaultAsyncExecutor;
import com.msm.core.action.hook.DefaultHookEngine;
import com.msm.core.action.hook.HookEngine;
import com.msm.core.action.transaction.TransactionHook;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.DefaultPredicateFactory;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.AuditStrategyResolverFactory;
import com.msm.core.objects.audit.DefaultAuditStrategy;
import com.msm.core.objects.config.DynamicRulesFactory;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.config.ObjectBeanConfigInitializing;
import com.msm.core.objects.config.provider.ObjectMetadataProvider;
import com.msm.core.objects.controller.GenericObjectController;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.converter.DefaultCustomValueMappingStrategy;
import com.msm.core.objects.converter.MappingStrategyResolverFactory;
import com.msm.core.objects.handler.GenericObjectHandler;
import com.msm.core.objects.hook.GenericHookEvent;
import com.msm.core.objects.hook.system.SystemHookEvent;
import com.msm.core.objects.repository.DefaultRepositoryFactory;
import com.msm.core.objects.repository.RepositoryFactory;
import com.msm.core.objects.rules.GenericObjectRulesService;
import com.msm.core.objects.service.DefaultSoftDeleteFilter;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.GenericObjectService;
import com.msm.core.objects.service.ObjectUsageConfig;
import com.msm.core.objects.service.PreprocessCustomFieldValueService;
import com.msm.core.objects.transaction.ObjectTransactionHook;
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
@EnableConfigurationProperties(GenericObjectConfigProperties.class)
public class MsmAutoConfiguration {

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

    @Bean
    @ConditionalOnMissingBean
    public DynamicQueryService dynamicQueryService(DSLContext dslContext) {
        return new DynamicQueryService(dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityClassFactory entityClassFactory(EntityManager entityManager) {
        return new EntityClassFactory(entityManager);
    }

    @Bean
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
    public ObjectBeanConfigInitializing objectBeanConfigInitializing(ApplicationContext applicationContext,
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
    public GenericHookEvent genericHookEvent(
            @Qualifier("defaultAttributeValidator") AttributeValidator defaultAttributeValidator,
            GenericObjectMetadataService genericObjectMetadataService) {
        return new GenericHookEvent(defaultAttributeValidator, genericObjectMetadataService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectUsageConfig objectUsageConfig(GenericObjectMetadataService genericObjectMetadataService) {
        return new ObjectUsageConfig(genericObjectMetadataService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemHookEvent systemHookEvent(ObjectUsageConfig objectUsageService) {
        return new SystemHookEvent(objectUsageService);
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
    public CustomValueMappingStrategy defaultObjectMappingStrategy() {
        return new DefaultCustomValueMappingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public MappingStrategyResolverFactory objectMappingStrategyFactory(List<CustomValueMappingStrategy> objectMappingStrategies, CustomValueMappingStrategy defaultCustomValueMappingStrategy) {
        return new MappingStrategyResolverFactory(objectMappingStrategies, defaultCustomValueMappingStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericObjectHandler genericObjectExecutor(
            DynamicQueryService dynamicQueryService,
            GenericObjectMetadataService genericObjectMetadataService,
            StrategyResolver<String, AuditStrategy> auditStrategyFactory,
            StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory
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
    public GenericObjectService genericObjectService(ActionExecutor actionExecutor) {
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

package com.msm.core.objects;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.action.executor.DefaultActionExecutor;
import com.msm.core.action.executor.DefaultAsyncExecutor;
import com.msm.core.action.hook.DefaultHookEngine;
import com.msm.core.action.hook.HookEngine;
import com.msm.core.action.transaction.TransactionHook;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.dynamicquery.command.DefaultDynamicDelete;
import com.msm.core.dynamicquery.command.DefaultDynamicInsert;
import com.msm.core.dynamicquery.command.DefaultDynamicUpdate;
import com.msm.core.dynamicquery.command.DynamicDelete;
import com.msm.core.dynamicquery.command.DynamicInsert;
import com.msm.core.dynamicquery.command.DynamicUpdate;
import com.msm.core.dynamicquery.query.DefaultDynamicFilterQuery;
import com.msm.core.dynamicquery.query.DynamicFilterQuery;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.DefaultPredicateFactory;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.AuditStrategyResolverFactory;
import com.msm.core.objects.audit.DefaultAuditStrategy;
import com.msm.core.objects.cache.InMemoryCaches;
import com.msm.core.objects.config.DynamicRulesFactory;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.config.ObjectBeanConfigInitializing;
import com.msm.core.objects.config.provider.ObjectMetadataProvider;
import com.msm.core.objects.connector.MasterDataApiService;
import com.msm.core.objects.controller.GenericObjectController;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.converter.DefaultCustomValueMappingStrategy;
import com.msm.core.objects.converter.MappingStrategyResolverFactory;
import com.msm.core.objects.handler.GenericObjectHandler;
import com.msm.core.objects.hook.GenericHookEvent;
import com.msm.core.objects.hook.system.SystemHookEvent;
import com.msm.core.objects.integration.DefaultRequestClient;
import com.msm.core.objects.integration.IntegrationClient;
import com.msm.core.objects.integration.IntegrationClientExchange;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.integration.auth.apikey.ApiKeyAuthProvider;
import com.msm.core.objects.integration.auth.apikey.ApiKeyQueryProvider;
import com.msm.core.objects.integration.auth.basic.BasicEncodedProvider;
import com.msm.core.objects.integration.auth.basic.BasicUsernamePasswordProvider;
import com.msm.core.objects.integration.auth.common.AuthProvider;
import com.msm.core.objects.integration.auth.common.TokenProvider;
import com.msm.core.objects.integration.auth.oauth2.CachedOAuth2TokenProvider;
import com.msm.core.objects.integration.auth.oauth2.OAuth2AuthProvider;
import com.msm.core.objects.integration.auth.oauth2.password.CachedOAuth2PasswordTokenProvider;
import com.msm.core.objects.integration.auth.oauth2.password.OAuth2PasswordAuthProvider;
import com.msm.core.objects.integration.data.retry.RetryDefaultProperties;
import com.msm.core.objects.integration.factory.AuthProviderFactory;
import com.msm.core.objects.integration.factory.TokenProviderFactory;
import com.msm.core.objects.integration.middleware.AuthMiddleware;
import com.msm.core.objects.integration.middleware.HttpMiddlewareChain;
import com.msm.core.objects.integration.middleware.Middleware;
import com.msm.core.objects.integration.middleware.TracingMiddleware;
import com.msm.core.objects.integration.retry.ResilienceRetryExecutor;
import com.msm.core.objects.integration.retry.RetryConfigResolver;
import com.msm.core.objects.integration.retry.RetryExecutor;
import com.msm.core.objects.repository.DefaultRepositoryFactory;
import com.msm.core.objects.repository.RepositoryFactory;
import com.msm.core.objects.rules.GenericObjectRulesService;
import com.msm.core.objects.service.DefaultSoftDeleteFilter;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.GenericObjectService;
import com.msm.core.objects.service.IntegrationLogService;
import com.msm.core.objects.service.ObjectDependencyServiceImpl;
import com.msm.core.objects.service.PreprocessCustomFieldValueService;
import com.msm.core.objects.transaction.ObjectTransactionHook;
import com.msm.core.strategy.StrategyResolver;
import com.msm.core.validate.attr.ValueValidationHandler;
import com.msm.core.validate.attr.rules.AttributeSimpleRule;
import com.msm.core.validate.validation.AttributeTypeValidator;
import com.msm.core.validate.validation.AttributeValidator;
import com.msm.core.validate.validation.DefaultAttributeValidator;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        GenericObjectConfigProperties.class,
        IntegrationProperties.class
})
public class MsmAutoConfiguration {

    @Bean
    public BeanPostProcessor msmEntityManagerFactoryPostProcessor() {
        System.out.println("=======> [MSM LIB] ĐÃ KHỞI TẠO BỘ CHẶN EM_FACTORY <=======");

        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof LocalContainerEntityManagerFactoryBean emfBean) {
                    List<String> entityClassNames = new ArrayList<>();
                    try {
                        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
                        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
                        String packagePath = "com/msm/core/objects/entity/integration/**/*.class";
                        Resource[] resources = resolver.getResources("classpath*:" + packagePath);
                        for (Resource resource : resources) {
                            if (resource.isReadable()) {
                                var reader = readerFactory.getMetadataReader(resource);
                                if (reader.getAnnotationMetadata().hasAnnotation(Entity.class.getName())) {
                                    entityClassNames.add(reader.getClassMetadata().getClassName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                    if (!entityClassNames.isEmpty()) {
                        emfBean.setPersistenceUnitPostProcessors(pui -> {
                            for (String className : entityClassNames) {
                                pui.addManagedClassName(className);
                            }
                        });
                    }
                }
                return bean;
            }
        };
    }

    @Bean(name = "hookTaskExecutor")
    @ConditionalOnMissingBean(name = "hookTaskExecutor")
    public Executor hookTaskExecutor(GenericObjectConfigProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getExecutor().getCore());
        executor.setMaxPoolSize(props.getExecutor().getMax());
        executor.setThreadNamePrefix("HookTaskExecutor-");
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean(name = "dynamicFilterQuery")
    @ConditionalOnMissingBean
    public DynamicFilterQuery dynamicFilterQuery(DSLContext dslContext) {
        return new DefaultDynamicFilterQuery(dslContext);
    }

    @Bean(name = "dynamicInsert")
    @ConditionalOnMissingBean
    public DynamicInsert dynamicInsert(DSLContext dslContext) {
        return new DefaultDynamicInsert(dslContext);
    }

    @Bean(name = "dynamicUpdate")
    @ConditionalOnMissingBean
    public DynamicUpdate dynamicUpdate(DSLContext dslContext) {
        return new DefaultDynamicUpdate(dslContext);
    }

    @Bean(name = "dynamicDelete")
    @ConditionalOnMissingBean
    public DynamicDelete dynamicDelete(DSLContext dslContext, @Qualifier("dynamicUpdate") DynamicUpdate dynamicUpdate) {
        return new DefaultDynamicDelete(dslContext, dynamicUpdate);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicQueryService dynamicQueryService(
            @Qualifier("dynamicFilterQuery") DynamicFilterQuery dynamicFilterQuery,
            @Qualifier("dynamicInsert") DynamicInsert dynamicInsert,
            @Qualifier("dynamicUpdate") DynamicUpdate dynamicUpdate,
            @Qualifier("dynamicDelete") DynamicDelete dynamicDelete) {
        return new DynamicQueryService(dynamicFilterQuery, dynamicInsert, dynamicUpdate, dynamicDelete);
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
    public HookEngine hookEngine(@Qualifier("hookTaskExecutor") Executor hookTaskExecutor) {
        return new DefaultHookEngine(new DefaultAsyncExecutor(hookTaskExecutor));
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public RequestDataValidator defaultRequestDataValidator(
//            @Qualifier("defaultAttributeValidator") AttributeValidator defaultAttributeValidator) {
//        return new DefaultRequestDataValidator(defaultAttributeValidator);
//    }

//    @Bean
//    @ConditionalOnMissingBean
//    public RequestDataProcessor defaultRequestDataProcessor(RequestDataValidator defaultRequestDataValidator) {
//        return new DefaultRequestDataProcessor(defaultRequestDataValidator);
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public RequestDataProcessorFactory requestDataProcessorFactory(List<RequestDataProcessor> requestDataProcessors, RequestDataProcessor defaultRequestDataProcessor) {
//        return new RequestDataProcessorFactory(requestDataProcessors, defaultRequestDataProcessor);
//    }

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
    public ObjectDependencyServiceImpl objectDependencyService(
            GenericObjectHandler genericObjectHandler,
            MasterDataApiService masterDataApiService
    ) {
        return new ObjectDependencyServiceImpl(genericObjectHandler, masterDataApiService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemHookEvent systemHookEvent(
            ObjectDependencyServiceImpl objectDependencyService,
            @Qualifier("defaultAttributeValidator") AttributeValidator defaultAttributeValidator,
            GenericObjectMetadataService genericObjectMetadataService
    ) {
        return new SystemHookEvent(objectDependencyService, defaultAttributeValidator, genericObjectMetadataService);
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

    @Bean
    @ConditionalOnMissingBean
    public MasterDataApiService masterDataApiService(@Qualifier("internalRestClient") RestClient restClient) {
        return new MasterDataApiService(restClient);
    }

    @Bean("internalRestClient")
    @ConditionalOnMissingBean
    @Qualifier("internalRestClient")
    public RestClient internalRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean(name = "internalRequestClient")
    @ConditionalOnMissingBean
    @Qualifier("internalRequestClient")
    RequestClient internalRequestClient(@Qualifier("internalRestClient") RestClient restClient) {
        return new DefaultRequestClient(restClient);
    }

    @Bean("integrationRestClient")
    @Qualifier("integrationRestClient")
    public RestClient integrationRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean(name = "integrationRequestClient")
    @Qualifier("integrationRequestClient")
    RequestClient integrationRequestClient(@Qualifier("integrationRestClient") RestClient restClient) {
        return new DefaultRequestClient(restClient);
    }



    @Bean
    public AuthProvider apiKeyAuthProvider() {
        return new ApiKeyAuthProvider();
    }

    @Bean
    public AuthProvider apiKeyQueryProvider() {
        return new ApiKeyQueryProvider();
    }

    @Bean
    public AuthProvider basicEncodedProvider() {
        return new BasicEncodedProvider();
    }

    @Bean
    public AuthProvider basicUsernamePasswordProvider() {
        return new BasicUsernamePasswordProvider();
    }

    @Bean
    public AuthProvider oAuth2AuthProvider(TokenProviderFactory tokenProviderFactory) {
        return new OAuth2AuthProvider(tokenProviderFactory);
    }

    @Bean
    public AuthProvider oAuth2PasswordAuthProvider(TokenProviderFactory tokenProviderFactory) {
        return new OAuth2PasswordAuthProvider(tokenProviderFactory);
    }

    @Bean
    public AuthProviderFactory authProviderRegistry(List<AuthProvider> authProviders) {
        return new AuthProviderFactory(authProviders);
    }

    @Bean
    AuthMiddleware authMiddleware(AuthProviderFactory authProviderFactory) {
        return new AuthMiddleware(authProviderFactory);
    }

    @Bean
    public TokenProvider cachedOAuth2TokenManager(
            @Qualifier("integrationRequestClient") RequestClient requestClient,
            RetryExecutor retryExecutor,
            IntegrationProperties integrationProperties
    ) {
        return new CachedOAuth2TokenProvider(requestClient, retryExecutor, integrationProperties);
    }

    @Bean
    public TokenProvider cachedOAuth2PasswordTokenManager(
            @Qualifier("integrationRequestClient") RequestClient requestClient,
            RetryExecutor retryExecutor,
            IntegrationProperties integrationProperties
    ) {
        return new CachedOAuth2PasswordTokenProvider(requestClient, retryExecutor, integrationProperties);
    }

    @Bean
    public TokenProviderFactory tokenProviderRegistry(List<TokenProvider> providers) {
        return new TokenProviderFactory(providers);
    }




    @Bean
    @ConditionalOnMissingBean
    TracingMiddleware tracingMiddleware(IntegrationLogService integrationLogService) {
        return new TracingMiddleware(integrationLogService);
    }

    @Bean
    @ConditionalOnMissingBean
    RetryConfigResolver retryConfigResolver(IntegrationProperties properties) {
        return new RetryConfigResolver(properties, new RetryDefaultProperties());
    }

    @Bean
    @ConditionalOnMissingBean
    RetryExecutor retryExecutor(RetryConfigResolver retryConfigResolver) {
        return new ResilienceRetryExecutor(retryConfigResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    HttpMiddlewareChain chain(List<Middleware> middlewares) {
        return new HttpMiddlewareChain(middlewares);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationClientExchange integrationClientExchange(
            @Qualifier("integrationRequestClient") RequestClient requestClient,
            HttpMiddlewareChain chain,
            RetryExecutor retryExecutor) {
        return new IntegrationClientExchange(requestClient, chain, retryExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    IntegrationClient integrationClient(
            IntegrationProperties integrationProperties,
            IntegrationClientExchange integrationClientExchange) {
        return new IntegrationClient(integrationProperties, integrationClientExchange);
    }

    @Bean
    @ConditionalOnMissingBean
    IntegrationLogService integrationService(GenericObjectHandler genericObjectHandler) {
        return new IntegrationLogService(genericObjectHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("IntegrationTokenCache", Caffeine.newBuilder()
                .maximumSize(1000)
                .build());
        return cacheManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryCaches inMemoryCaches(CacheManager cacheManager) {
        return new InMemoryCaches(cacheManager);
    }

}

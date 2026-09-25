package com.msm.core.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.action.executor.DefaultActionExecutor;
import com.msm.core.action.executor.DefaultAsyncExecutor;
import com.msm.core.action.hook.DefaultHookEngine;
import com.msm.core.action.hook.HookEngine;
import com.msm.core.action.transaction.TransactionHook;
import com.msm.core.dynamicquery.DefaultQueryService;
import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.dynamicquery.command.DefaultDynamicDelete;
import com.msm.core.dynamicquery.command.DefaultDynamicInsert;
import com.msm.core.dynamicquery.command.DefaultDynamicUpdate;
import com.msm.core.dynamicquery.command.DynamicDelete;
import com.msm.core.dynamicquery.command.DynamicInsert;
import com.msm.core.dynamicquery.command.DynamicUpdate;
import com.msm.core.dynamicquery.internal.InternalFilterQuery;
import com.msm.core.dynamicquery.internal.InternalQueryService;
import com.msm.core.dynamicquery.query.DefaultFilterQuery;
import com.msm.core.dynamicquery.query.FilterQuery;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.DefaultPredicateFactory;
import com.msm.core.filter.EntityClassFactory;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.AuditStrategyResolverFactory;
import com.msm.core.objects.audit.DefaultAuditStrategy;
import com.msm.core.objects.cache.InMemoryCaches;
import com.msm.core.objects.cache.RedisCacheOperator;
import com.msm.core.objects.cache.RedisCacheOperatorService;
import com.msm.core.objects.config.DynamicRulesFactory;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.config.IntegrationProperties;
import com.msm.core.objects.config.ObjectBeanConfigInitializing;
import com.msm.core.objects.config.ObjectExportRegistry;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.config.S3PropConfig;
import com.msm.core.objects.config.provider.ObjectMetadataProvider;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.connector.MasterDataApiService;
import com.msm.core.objects.controller.GenericObjectController;
import com.msm.core.objects.controller.InternalGenericObjectController;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.converter.DefaultCustomValueMappingStrategy;
import com.msm.core.objects.converter.MappingStrategyResolverFactory;
import com.msm.core.objects.dataexchange.exports.ExportConfigService;
import com.msm.core.objects.dataexchange.exports.ExportJobService;
import com.msm.core.objects.dataexchange.exports.ExportJobTransactionService;
import com.msm.core.objects.dataexchange.exports.excel.ExportExcelService;
import com.msm.core.objects.dataexchange.exports.excel.ExportExcelTemplateService;
import com.msm.core.objects.dataexchange.exports.excel.handler.ExportExcelHandler;
import com.msm.core.objects.dataexchange.imports.BatchImportService;
import com.msm.core.objects.dataexchange.imports.ImportConfigService;
import com.msm.core.objects.dataexchange.imports.ImportDataProcessor;
import com.msm.core.objects.dataexchange.imports.ImportDataService;
import com.msm.core.objects.dataexchange.imports.ImportDataTransactionExecutor;
import com.msm.core.objects.dataexchange.imports.ImportErrorService;
import com.msm.core.objects.dataexchange.imports.ImportJobService;
import com.msm.core.objects.dataexchange.imports.ImportValidationService;
import com.msm.core.objects.dataexchange.imports.ReferenceProcessService;
import com.msm.core.objects.dataexchange.imports.config.ImportConfigLoader;
import com.msm.core.objects.dataexchange.imports.csv.CsvImportService;
import com.msm.core.objects.dataexchange.imports.csv.FileReaderService;
import com.msm.core.objects.dataexchange.imports.csv.handler.CsvImportHandlerService;
import com.msm.core.objects.dataexchange.imports.excel.ImportDataExecutor;
import com.msm.core.objects.dataexchange.imports.excel.ImportExcelService;
import com.msm.core.objects.dataexchange.imports.excel.handler.ImportExcelHandler;
import com.msm.core.objects.dataexchange.imports.reference.AttributeCodeReferenceResolver;
import com.msm.core.objects.dataexchange.imports.reference.AttributeReferenceResolver;
import com.msm.core.objects.dataexchange.imports.s3.ExcelOriginalMultipartAsyncService;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
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
import com.msm.core.objects.integration.auth.bearer.StaticBearerAuthProvider;
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
import com.msm.core.objects.integration.retry.ExchangeRetryExecutor;
import com.msm.core.objects.integration.retry.HandleRequestRetryExecutor;
import com.msm.core.objects.integration.retry.RetryConfigResolver;
import com.msm.core.objects.integration.retry.RetryExecutor;
import com.msm.core.objects.logging.GenericBaseExceptionResolver;
import com.msm.core.objects.logging.IntegrationErrorResolver;
import com.msm.core.objects.logging.IntegrationLogWriter;
import com.msm.core.objects.logging.IntegrationLoggingAspect;
import com.msm.core.objects.repository.DefaultObjectQueryRepository;
import com.msm.core.objects.repository.DefaultRepositoryFactory;
import com.msm.core.objects.repository.InternalObjectQueryRepository;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.repository.RepositoryFactory;
import com.msm.core.objects.rules.GenericObjectRulesService;
import com.msm.core.objects.security.DefaultSecurityFieldResolver;
import com.msm.core.objects.security.SecurityFieldResolver;
import com.msm.core.objects.security.SecurityFieldResolverFactory;
import com.msm.core.objects.service.DefaultSoftDeleteFilter;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.objects.service.GenericObjectService;
import com.msm.core.objects.service.IntegrationLogService;
import com.msm.core.objects.service.ObjectDependencyServiceImpl;
import com.msm.core.objects.service.PermissionService;
import com.msm.core.objects.service.PreprocessCustomFieldValueService;
import com.msm.core.objects.service.ValidateAndPopulateDataService;
import com.msm.core.objects.service.internal.InternalGenericObjectService;
import com.msm.core.objects.transaction.ObjectTransactionHook;
import com.msm.core.security.DataScopeConditionResolver;
import com.msm.core.security.DataScopeResolver;
import com.msm.core.security.SecurityCheckProvider;
import com.msm.core.security.SecurityConditionProvider;
import com.msm.core.strategy.StrategyResolver;
import com.msm.core.validate.attr.ValueValidationHandler;
import com.msm.core.validate.attr.rules.AttributeSimpleRule;
import com.msm.core.validate.validation.AttributeTypeValidator;
import com.msm.core.validate.validation.AttributeValidationSupport;
import com.msm.core.validate.validation.AttributeValidator;
import com.msm.core.validate.validation.CreateAttributeValidator;
import com.msm.core.validate.validation.DefaultAttributeValidator;
import com.msm.core.validate.validation.UpdateAttributeValidator;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        GenericObjectConfigProperties.class,
        IntegrationProperties.class,
        ObjectImportRegistry.class,
        ObjectExportRegistry.class,
        S3PropConfig.class
})
public class MsmAutoConfiguration {

    private List<String> injectEntities(String packagePath) {
        List<String> entityClassNames = new ArrayList<>();
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
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

        return entityClassNames;
    }



    @Bean
    public BeanPostProcessor msmEntityManagerFactoryPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof LocalContainerEntityManagerFactoryBean emfBean) {
//                    List<String> entityClassNames = new ArrayList<>();
//                    try {
//                        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
//                        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);
//                        String packagePath = "com/msm/core/objects/entity/integration/**/*.class";
//                        Resource[] resources = resolver.getResources("classpath*:" + packagePath);
//                        for (Resource resource : resources) {
//                            if (resource.isReadable()) {
//                                var reader = readerFactory.getMetadataReader(resource);
//                                if (reader.getAnnotationMetadata().hasAnnotation(Entity.class.getName())) {
//                                    entityClassNames.add(reader.getClassMetadata().getClassName());
//                                }
//                            }
//                        }
//                    } catch (Exception e) {
//                        log.error(e.getMessage(), e);
//                    }
//
//                    if (!entityClassNames.isEmpty()) {
//                        emfBean.setPersistenceUnitPostProcessors(pui -> {
//                            for (String className : entityClassNames) {
//                                pui.addManagedClassName(className);
//                            }
//                        });
//                    }

                    List<String> entityClassNames = injectEntities("com/msm/core/objects/entity/**/*.class");
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

    @Bean
    public ImportConfigLoader yamlConfigLoader() {
        return new ImportConfigLoader();
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

    @Bean(name = "dataExchangeTaskExecutor")
    @ConditionalOnMissingBean(name = "dataExchangeTaskExecutor")
    public Executor importExportDataTaskExecutor(GenericObjectConfigProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getExecutor().getCore());
        executor.setMaxPoolSize(props.getExecutor().getMax());
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("dataExchangeTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataScopeResolver dataScopeResolver() {
        return new DataScopeConditionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityConditionProvider securityConditionProvider(DataScopeResolver dataScopeResolver) {
        return new SecurityConditionProvider(dataScopeResolver);
    }

    @Bean(name = "defaultFilterQuery")
    @ConditionalOnMissingBean
    public FilterQuery defaultFilterQuery(
            DSLContext dslContext,
            SecurityConditionProvider securityConditionProvider) {
        return new DefaultFilterQuery(dslContext, securityConditionProvider);
    }

    @Bean(name = "internalFilterQuery")
    public FilterQuery internalFilterQuery(DSLContext dslContext) {
        return new InternalFilterQuery(dslContext);
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

    @Bean("defaultQueryService")
    public ObjectQuery defaultQueryService(
            @Qualifier("defaultFilterQuery") FilterQuery defaultFilterQuery,
            @Qualifier("dynamicInsert") DynamicInsert dynamicInsert,
            @Qualifier("dynamicUpdate") DynamicUpdate dynamicUpdate,
            @Qualifier("dynamicDelete") DynamicDelete dynamicDelete) {
        return new DefaultQueryService(defaultFilterQuery, dynamicInsert, dynamicUpdate, dynamicDelete);
    }

    @Bean("internalQueryService")
    public ObjectQuery internalQueryService(
            @Qualifier("internalFilterQuery") FilterQuery internalFilterQuery,
            @Qualifier("dynamicInsert") DynamicInsert dynamicInsert,
            @Qualifier("dynamicUpdate") DynamicUpdate dynamicUpdate,
            @Qualifier("dynamicDelete") DynamicDelete dynamicDelete) {
        return new InternalQueryService(internalFilterQuery, dynamicInsert, dynamicUpdate, dynamicDelete);
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

    @Bean(name = "attributeValidationSupport")
    @ConditionalOnMissingBean(name = "attributeValidationSupport")
    public AttributeValidationSupport attributeValidationSupport() {
        return new AttributeValidationSupport();
    }

    @Bean(name = "defaultAttributeValidator")
    @ConditionalOnMissingBean(name = "defaultAttributeValidator")
    public AttributeValidator defaultAttributeValidator(
            @Qualifier("attributeTypeValidator") AttributeValidator attributeTypeValidator) {
        return new DefaultAttributeValidator(attributeTypeValidator);
    }

    @Bean(name = "createAttributeValidator")
    @ConditionalOnMissingBean(name = "createAttributeValidator")
    public AttributeValidator createAttributeValidator(
            @Qualifier("attributeTypeValidator") AttributeValidator attributeTypeValidator,
            AttributeValidationSupport attributeValidationSupport) {
        return new CreateAttributeValidator(attributeTypeValidator, attributeValidationSupport);
    }

    @Bean(name = "updateAttributeValidator")
    @ConditionalOnMissingBean(name = "updateAttributeValidator")
    public AttributeValidator updateAttributeValidator(
            @Qualifier("attributeTypeValidator") AttributeValidator attributeTypeValidator,
            AttributeValidationSupport attributeValidationSupport
    ) {
        return new UpdateAttributeValidator(attributeTypeValidator, attributeValidationSupport);
    }

    @Bean(name = "validateAndPopulateDataService")
    @ConditionalOnMissingBean
    public ValidateAndPopulateDataService validateAndPopulateDataService(
            @Qualifier("createAttributeValidator") AttributeValidator createAttributeValidator,
            @Qualifier("updateAttributeValidator") AttributeValidator updateAttributeValidator) {
        return new ValidateAndPopulateDataService(createAttributeValidator, updateAttributeValidator);
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
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            MasterDataApiService masterDataApiService
    ) {
        return new ObjectDependencyServiceImpl(internalObjectQueryRepository, masterDataApiService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SystemHookEvent systemHookEvent(
            GenericObjectMetadataService genericObjectMetadataService,
            ValidateAndPopulateDataService validateAndPopulateDataService
    ) {
        return new SystemHookEvent(genericObjectMetadataService, validateAndPopulateDataService);
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

    @Bean("objectQueryRepository")
    public ObjectQueryRepository defaultObjectQueryRepository(
            @Qualifier("defaultQueryService") ObjectQuery defaultQueryService,
            GenericObjectMetadataService genericObjectMetadataService,
            StrategyResolver<String, AuditStrategy> auditStrategyFactory,
            StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory
    ) {
        return new DefaultObjectQueryRepository(defaultQueryService, genericObjectMetadataService, auditStrategyFactory, objectMappingStrategyFactory);
    }

    @Bean("internalObjectQueryRepository")
    public ObjectQueryRepository internalObjectQueryRepository(
            @Qualifier("internalQueryService") ObjectQuery internalQueryService,
            GenericObjectMetadataService genericObjectMetadataService,
            StrategyResolver<String, AuditStrategy> auditStrategyFactory,
            StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory
    ) {
        return new InternalObjectQueryRepository(internalQueryService, genericObjectMetadataService, auditStrategyFactory, objectMappingStrategyFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericObjectHandler genericObjectExecutor(
            @Qualifier("objectQueryRepository") ObjectQueryRepository objectQueryRepository,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository) {
        return new GenericObjectHandler(objectQueryRepository, internalObjectQueryRepository);
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
    public InternalGenericObjectService internalGenericObjectService(ActionExecutor actionExecutor) {
        return new InternalGenericObjectService(actionExecutor);
    }

    @Bean
    public InternalGenericObjectController internalGenericObjectController(InternalGenericObjectService internalGenericObjectService, GenericObjectMetadataService genericObjectMetadataService) {
        return new InternalGenericObjectController(internalGenericObjectService, genericObjectMetadataService);
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
    public StaticBearerAuthProvider staticBearerAuthProvider() {
        return new StaticBearerAuthProvider();
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
            @Qualifier("handleRequestRetry") RetryExecutor handleRequestRetry,
            IntegrationProperties integrationProperties,
            @Qualifier("redisCacheOperatorService") RedisCacheOperator redisCacheOperator
    ) {
        return new CachedOAuth2TokenProvider(requestClient, handleRequestRetry, integrationProperties, redisCacheOperator);
    }

    @Bean
    public TokenProvider cachedOAuth2PasswordTokenManager(
            @Qualifier("integrationRequestClient") RequestClient requestClient,
            @Qualifier("handleRequestRetry") RetryExecutor handleRequestRetry,
            IntegrationProperties integrationProperties,
            @Qualifier("redisCacheOperatorService") RedisCacheOperator redisCacheOperator
    ) {
        return new CachedOAuth2PasswordTokenProvider(requestClient, handleRequestRetry, integrationProperties, redisCacheOperator);
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

    @Bean("handleRequestRetry")
    RetryExecutor handleRequestRetry(RetryConfigResolver retryConfigResolver) {
        return new HandleRequestRetryExecutor(retryConfigResolver);
    }

    @Bean("exchangeRetryExecutor")
    RetryExecutor exchangeRetryExecutor(RetryConfigResolver retryConfigResolver) {
        return new ExchangeRetryExecutor(retryConfigResolver);
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
            @Qualifier("handleRequestRetry") RetryExecutor handleRequestRetry,
            @Qualifier("exchangeRetryExecutor") RetryExecutor exchangeRetryExecutor) {
        return new IntegrationClientExchange(requestClient, chain, handleRequestRetry, exchangeRetryExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationClient integrationClient(
            IntegrationProperties integrationProperties,
            IntegrationClientExchange integrationClientExchange) {
        return new IntegrationClient(integrationProperties, integrationClientExchange);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationLogService integrationService(
            ActionExecutor actionExecutor
    ) {
        return new IntegrationLogService(actionExecutor);
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



    //============ Import ===========

    @Bean("importValidationService0")
    public com.msm.core.objects.dataexchange.imports.validation.ImportValidationService importValidationService0(
            @Qualifier("createAttributeValidator") AttributeValidator createAttributeValidator,
            @Qualifier("updateAttributeValidator") AttributeValidator updateAttributeValidator
    ) {
        return new com.msm.core.objects.dataexchange.imports.validation.ImportValidationService(
                createAttributeValidator,
                updateAttributeValidator
        );
    }


    @Bean("referenceProcessService")
    public ReferenceProcessService referenceProcessService(
            ActionExecutor actionExecutor,
            ImportConfigService importConfigService
    ) {
        return new ReferenceProcessService(
                actionExecutor,
                importConfigService
        );
    }

    @Bean("batchProcessingService")
    public ImportValidationService batchProcessingService(
            @Qualifier("importValidationService0") com.msm.core.objects.dataexchange.imports.validation.ImportValidationService importValidationService0,
            ActionExecutor actionExecutor,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ImportConfigService importConfigService
    ) {
        return new ImportValidationService(
                importValidationService0,
                actionExecutor,
                internalObjectQueryRepository,
                importConfigService
        );
    }



    @Bean("importService")
    public CsvImportService importService(
            BatchImportService batchImportService,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ActionExecutor actionExecutor,
            ReferenceProcessService referenceProcessService,
            S3FileUtils s3FileUtils,
            ImportConfigService importConfigService
    ) {
        return new CsvImportService(
                batchImportService,
                internalObjectQueryRepository,
                actionExecutor,
                referenceProcessService,
                s3FileUtils,
                importConfigService
        );
    }


    @Bean("fileReaderService")
    public FileReaderService fileReaderService(
            ActionExecutor actionExecutor
    ) {
        return new FileReaderService(
                actionExecutor
        );
    }

    @Bean("csvImportHandlerService")
    public CsvImportHandlerService csvImportHandlerService(
            ImportValidationService processValidationBatch,
            FileReaderService fileReaderService,
            AttributeReferenceResolver attributeReferenceResolver,
            ImportConfigService importConfigService
    ) {
        return new CsvImportHandlerService(
                processValidationBatch,
                fileReaderService,
                attributeReferenceResolver,
                importConfigService
        );
    }

    @Bean("attributeReferenceResolverService")
    public AttributeCodeReferenceResolver attributeReferenceResolverService(
            GenericObjectInternalService genericObjectInternalService,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ImportConfigService importConfigService
    ) {
        return new AttributeCodeReferenceResolver(
                genericObjectInternalService,
                internalObjectQueryRepository,
                importConfigService
        );
    }


    @Bean("typeAndCodeReferenceResolver")
    public AttributeReferenceResolver typeAndCodeReferenceResolver(
            GenericObjectInternalService genericObjectInternalService,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ImportConfigService importConfigService
    ) {
        return new AttributeReferenceResolver(
                genericObjectInternalService,
                internalObjectQueryRepository,
                importConfigService
        );
    }


    @Bean("importConfigService")
    public ImportConfigService importConfigService(
            ObjectImportRegistry objectImportRegistry
    ) {
        return new ImportConfigService(
                objectImportRegistry
        );
    }

    @Bean("exportConfigService")
    public ExportConfigService exportConfigService(
            ObjectExportRegistry objectExportRegistry
    ) {
        return new ExportConfigService(
                objectExportRegistry
        );
    }


    @Bean("s3Client")
    @ConditionalOnMissingBean
    @Qualifier("s3Client")
    public S3Client s3Client() {
        return S3Client.create();
    }


    @Bean("excelOriginalMultipartAsyncService")
    public ExcelOriginalMultipartAsyncService excelOriginalMultipartAsyncService(
            GenericObjectInternalService genericObjectInternalService,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            S3Client s3Client,
            S3FileUtils s3FileUtils
    ) {
        return new ExcelOriginalMultipartAsyncService(
                genericObjectInternalService,
                internalObjectQueryRepository,
                s3Client,
                s3FileUtils
        );
    }

    @Bean("importDataExecutor")
    public ImportDataExecutor importDataExecutor(
            ActionExecutor actionExecutor
    ) {
        return new ImportDataExecutor(actionExecutor);
    }

    //Excel
    @Bean("excelImportService")
    public ImportExcelService excelImportService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ActionExecutor actionExecutor,
            GenericObjectConfigProperties config,
            ReferenceProcessService referenceProcessService,
            ExcelOriginalMultipartAsyncService excelOriginalMultipartAsyncService,
            S3FileUtils s3FileUtils,
            ImportConfigService importConfigService,
            ImportDataExecutor importDataExecutor,
            ImportJobService importJobService,
            ImportErrorService importErrorService
    ) {
        return new ImportExcelService(
                internalObjectQueryRepository,
                actionExecutor,
                referenceProcessService,
                excelOriginalMultipartAsyncService,
                s3FileUtils,
                importConfigService,
                importDataExecutor,
                importJobService,
                importErrorService
        );
    }

    @Bean("excelImportHandlerService")
    public ImportExcelHandler excelImportHandlerService(
            ImportValidationService processValidationBatch,
            FileReaderService fileReaderService,
            AttributeReferenceResolver attributeReferenceResolver,
            ImportConfigService importConfigService,
            ImportDataService importDataService
    ) {
        return new ImportExcelHandler(
                processValidationBatch,
                fileReaderService,
                attributeReferenceResolver,
                importConfigService,
                importDataService
        );
    }

    @Bean("exportExcelHandlerService")
    public ExportExcelHandler exportExcelHandlerService(
    ) {
        return new ExportExcelHandler();
    }

    @Bean("excelTemplateService")
    public ExportExcelTemplateService excelTemplateService(
            S3Client s3Client,
            ActionExecutor actionExecutor
    ) {
        return new ExportExcelTemplateService(
                s3Client,
                actionExecutor
        );
    }

    @Bean("exportJobTransactionService")
    public ExportJobTransactionService exportJobTransactionService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository
    ) {
        return new ExportJobTransactionService(
                internalObjectQueryRepository
        );
    }

    @Bean("exportExcelService")
    public ExportExcelService exportExcelService(
            ExportExcelTemplateService exportExcelTemplateService,
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            S3Client s3Client,
            S3FileUtils s3FileUtils,
            ActionExecutor actionExecutor,
            ExportJobTransactionService exportJobTransactionService,
            ExportConfigService exportConfigService
    ) {
        return new ExportExcelService(
                exportExcelTemplateService,
                internalObjectQueryRepository,
                s3Client,
                s3FileUtils,
                actionExecutor,
                exportJobTransactionService,
                exportConfigService
        );
    }


    @Bean("s3FileUtils")
    public S3FileUtils s3FileUtils(
            S3Presigner s3Presigner,
            S3PropConfig s3PropConfig
    ) {
        return new S3FileUtils(
                s3Presigner,
                s3PropConfig
        );
    }

    @Bean("importJobService")
    public ImportJobService importJobService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            GenericObjectInternalService genericObjectInternalService,
            S3FileUtils s3FileUtils
    ) {
        return new ImportJobService(
                genericObjectInternalService,
                internalObjectQueryRepository,
                s3FileUtils
        );
    }

    @ConditionalOnMissingBean
    @Bean("exportJobService")
    public ExportJobService exportJobService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ExportExcelService exportExcelService,
            S3FileUtils s3FileUtils
    ) {
        return new ExportJobService(
                internalObjectQueryRepository,
                exportExcelService,
                s3FileUtils
        );
    }

    @Bean("importErrorService")
    public ImportErrorService importErrorService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository
    ) {
        return new ImportErrorService(internalObjectQueryRepository);
    }

    @Bean("batchImportService")
    public BatchImportService batchImportService(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ImportErrorService importErrorService,
            ImportConfigService importConfigService,
            ImportDataExecutor importDataExecutor
    ) {
        return new BatchImportService(
                importErrorService,
                internalObjectQueryRepository,
                importConfigService,
                importDataExecutor
        );
    }

    @Bean("batchImportServiceV2")
    public ImportDataService batchImportServiceV2(
            @Qualifier("internalObjectQueryRepository") ObjectQueryRepository internalObjectQueryRepository,
            ImportErrorService importErrorService,
            ImportConfigService importConfigService,
            ImportDataProcessor importDataProcessor
    ) {
        return new ImportDataService(
                importErrorService,
                internalObjectQueryRepository,
                importConfigService,
                importDataProcessor
        );
    }
    //ProcessInsertUpdateBatch
    @Bean("processInsertUpdateBatch")
    public ImportDataProcessor processInsertUpdateBatch(
            ImportDataTransactionExecutor transactionExecutor
    ) {
        return new ImportDataProcessor(
                transactionExecutor
        );
    }

    @Bean("importTransactionExecutor")
    public ImportDataTransactionExecutor importTransactionExecutor(
            ActionExecutor actionExecutor
    ) {
        return new ImportDataTransactionExecutor(
                actionExecutor
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericObjectInternalService genericObjectInternalService(
            GenericObjectConfigProperties genericObjectConfigProperties,
            @Qualifier("internalRequestClient") RequestClient requestClient
    ) {
        return new GenericObjectInternalService(genericObjectConfigProperties, requestClient);
    }


    @Bean("defaultSecurityFieldResolver")
    public SecurityFieldResolver defaultSecurityFieldResolver() {
        return new DefaultSecurityFieldResolver();
    }

    @Bean
    public SecurityFieldResolverFactory securityFieldResolverFactory(
            List<SecurityFieldResolver> securityFieldResolvers,
            @Qualifier("defaultSecurityFieldResolver") SecurityFieldResolver defaultSecurityFieldResolver) {
        return new SecurityFieldResolverFactory(securityFieldResolvers, defaultSecurityFieldResolver);
    }


    @Bean
    public PermissionService permissionService(
            DSLContext dslContext,
            SecurityCheckProvider securityCheckProvider,
            DataScopeResolver dataScopeResolver,
            SecurityFieldResolverFactory securityFieldResolverFactory) {
        return new PermissionService(dslContext, securityCheckProvider, dataScopeResolver, securityFieldResolverFactory);
    }

    @Bean
    public SecurityCheckProvider securityCheckProvider(
            DSLContext dslContext,
            DataScopeResolver dataScopeResolver) {
        return new SecurityCheckProvider(dslContext, dataScopeResolver);
    }

    @Bean("redisCacheOperatorService")
    @ConditionalOnMissingBean
    public RedisCacheOperator redisCacheOperator(
            @Qualifier("redisTemplateService") RedisTemplate<String, String> redisTemplate,
            StringRedisTemplate stringRedisTemplate) {
        return new RedisCacheOperatorService(redisTemplate, stringRedisTemplate);
    }

    @Bean("redisTemplateService")
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public GenericBaseExceptionResolver genericBaseExceptionResolver() {
        return new GenericBaseExceptionResolver();
    }

    @Bean
    public IntegrationLogWriter integrationLogWriter(IntegrationLogService integrationLogService) {
        return new IntegrationLogWriter(integrationLogService);
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationLoggingAspect integrationLoggingAspect(
            IntegrationLogWriter integrationLogWriter,
            ObjectMapper objectMapper,
            Environment environment,
            List<IntegrationErrorResolver> errorResolvers) {

        return new IntegrationLoggingAspect(
                integrationLogWriter,
                objectMapper,
                environment,
                errorResolvers
        );
    }
}

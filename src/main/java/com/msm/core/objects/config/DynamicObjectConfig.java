package com.msm.core.objects.config;

//@Configuration
public class DynamicObjectConfig {

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
//    public DynamicQueryService dynamicQueryService(DSLContext dsl) {
//        return new DynamicQueryService(dsl);
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
}

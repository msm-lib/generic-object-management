package com.msm.core.objects.repository;

import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.objects.audit.AuditStrategy;
import com.msm.core.objects.audit.changelog.ChangeLogRecorder;
import com.msm.core.objects.audit.changelog.UniqueConstraintColumns;
import com.msm.core.objects.converter.CustomValueMappingStrategy;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.msm.core.strategy.StrategyResolver;

/**
 * The repository over the internal {@link ObjectQuery} (no data-scope filter). Kept as its own type for services that
 * inject it by class; the code is {@link DefaultObjectQueryRepository}.
 */
public class InternalObjectQueryRepository extends DefaultObjectQueryRepository {

    public InternalObjectQueryRepository(ObjectQuery internalQueryService,
                                         GenericObjectMetadataService genericObjectMetadataService,
                                         StrategyResolver<String, AuditStrategy> auditStrategyFactory,
                                         StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory,
                                         ChangeLogRecorder changeLogRecorder,
                                         UniqueConstraintColumns uniqueConstraintColumns) {
        super(internalQueryService, genericObjectMetadataService, auditStrategyFactory, objectMappingStrategyFactory,
                changeLogRecorder, internalQueryService, uniqueConstraintColumns);
    }

    /** Without change log. */
    public InternalObjectQueryRepository(ObjectQuery internalQueryService,
                                         GenericObjectMetadataService genericObjectMetadataService,
                                         StrategyResolver<String, AuditStrategy> auditStrategyFactory,
                                         StrategyResolver<String, CustomValueMappingStrategy> objectMappingStrategyFactory) {
        super(internalQueryService, genericObjectMetadataService, auditStrategyFactory, objectMappingStrategyFactory);
    }
}

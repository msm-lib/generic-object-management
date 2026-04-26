package com.msm.core.objects.generic.service;

import com.msm.core.commons.Constants;
import com.msm.core.commons.ObjectValueConverter;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.DynamicQueryService;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.dynamicquery.UserQuerySecurityContext;
import com.msm.core.filter.AdvancedFilterService;
import com.msm.core.filter.domain.*;
import com.msm.core.hook.anontation.Handler;
import com.msm.core.hook.common.ActionExecutor;
import com.msm.core.hook.context.ActionRequest;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.Errors;
import com.msm.core.objects.exception.ServiceErrorEnum;
import com.msm.core.objects.generic.ObjectConstants;
import com.msm.core.objects.generic.entity.AuditingEntity;
import com.msm.core.objects.generic.entity.SoftDeleteEntity;
import com.msm.core.objects.generic.repository.RepositoryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unchecked"})
@Component
@RequiredArgsConstructor
public class GenericObjectExecutor {
    private final ObjectValueConverter<Map<String, Object>> genericObjectConverter;
    private final RepositoryFactory repositoryFactory;
    private final AdvancedFilterService filterService;
    private final DefaultSoftDeleteFilter defaultSoftDeleteFilter;
    private final ActionExecutor actionExecutor;
    private final DynamicQueryService dynamicQueryService;

    @Handler(action = Constants.GENERIC_FILTER_ACTION)
    public <X> X filter(ActionRequest<ObjectFilterRequest> request) {
        ObjectFilterRequest objectFilterRequest = request.getPayload();
        defaultSoftDeleteFilter.addDefaultFilter(objectFilterRequest);
        PageResponse<Object> pageResponse = filterService.filter(objectFilterRequest);
        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(this::preprocessObject);
        return (X) pageResponse;
    }

    @Handler(action = Constants.GENERIC_ALL_OBJECT_ACTION)
    public <X> X findAllObject(ActionRequest<ObjectFilterRequest> request) {
        ObjectFilterRequest objectFilterRequest = request.getPayload();
        PageResponse<Object> pageResponse = filterService.filter(objectFilterRequest);
        List<Object> objects = pageResponse.getContents();
        Utils.CL.emptyIfNull(objects).forEach(this::preprocessObject);
        return (X) objects;
    }

    @Handler(action = Constants.Action.CREATE)
    public <X> X create(ActionRequest<Map<String, Object>> request) {
        Object code = request.getPayload().get("code");
        if(Objects.isNull(code)) {
            String prefix = Utils.STR.defaultIfBlank(ObjectConstants.PREFIX_OBJECT_CODE.get(Utils.STR.lowCase(request.getObjectName())), () -> "");
            int len = Utils.STR.isEmpty(prefix) ? 8 : 7;
            request.getPayload().put("code", Utils.toCodeGenerator(prefix, len));
        }
        JpaRepository<X, ?> jpaRepository = getJpaRepository(request);
        X object0 = genericObjectConverter.convert(request.getObjectName(), request.getPayload(), null);
        if(object0 instanceof AuditingEntity auditingEntity) {
            auditingEntity.createdAuditingEntity(UserQuerySecurityContext.getUserId(), UserQuerySecurityContext.getUsername());
        }
        return jpaRepository.saveAndFlush(object0);
    }

    @Handler(action = Constants.Action.UPDATE)
    public <X> X update(ActionRequest<Map<String, Object>> request) {
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadata(request.getObjectName());
        dynamicQueryService.updateById(objectMetadata, request.getObjectId(), request.getPayload());
//        JpaRepository<X, ?> jpaRepository = getJpaRepository(request);
//        X oldData = getObjectById(request.getObjectName(), request.getObjectId());
//        if(oldData == null) {
//            throw Errors.throwException(ServiceErrorEnum.NOT_FOUND, request.getObjectId());
//        }
//        try {
//            Utils.O.updateValues(oldData, request.getPayload());
//            GenericObjectUtils.updateRef(oldData, request.getPayload());
//            if(oldData instanceof AuditingEntity auditingEntity) {
//                auditingEntity.updatedAuditingEntity(SecurityUtils.getUserId(), SecurityUtils.getUsername());
//            }
//        } catch (JsonMappingException e) {
//            throw Errors.throwException(ServiceErrorEnum.INVALID_JSON_FORMAT);
//        }
//        return jpaRepository.save(oldData);

        return (X) request.getPayload();
    }

    @Handler(action = Constants.Action.DELETE)
    public <X> X delete(ActionRequest<Map<String, Object>> request) {
        JpaRepository<X, UUID> jpaRepository = (JpaRepository<X, UUID>) getJpaRepository(request);
        UUID id = (UUID) request.getObjectId();
        X entity = jpaRepository.findById(id).orElseThrow(() -> Errors.throwException(ServiceErrorEnum.NOT_FOUND, id));
        if (entity instanceof SoftDeleteEntity softDeleteEntity) {
            softDeleteEntity.softDelete(UserQuerySecurityContext.getUsername(), UserQuerySecurityContext.getUserId());
            jpaRepository.save(entity);
        } else {
            jpaRepository.deleteById(id);
        }

        return null;
    }

    private <X> JpaRepository<X, ?> getJpaRepository(ActionRequest<Map<String, Object>> request) {
        return repositoryFactory.getRepository(request.getObjectName());
    }

    private <X> X getObjectById(String objectName, Object valId) {
        ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                .builder()
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(Utils.CL.newArrayList(FilterCondition.create("id", FilterOperator.EQUALS, valId))).build()).build();
        defaultSoftDeleteFilter.addDefaultFilter(objectFilterRequest);
        PageResponse<Map<String, Object>> result = filterService.filter(objectFilterRequest);
        Class<?> returnClass = filterService.getEntityClassFactory().getEntityType(objectName).getJavaType();
        return (X)(Utils.CL.isNotEmpty(result.getContents()) ? Utils.O.toObject(result.getContents().getFirst(), returnClass) : null);
    }

    private void preprocessObject(Object object) {
        if(Map.class.isAssignableFrom(object.getClass())) {
            Map<String, Object> objectMap = (Map<String, Object>) object;
            ActionRequest<Map<String, Object>> request = ActionRequest.<Map<String, Object>>builder()
                    .action(ObjectConstants.UNWRAPPED_CUSTOM_VALUES)
                    .payload(objectMap)
                    .disableHookEvent(true)
                    .build();
            actionExecutor.execute(request);
        }
    }
}

package com.msm.core.objects.service;

import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.dynamicquery.SelectBuilder;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.dto.ObjectDeleteRequest;
import com.msm.core.security.ObjectAccessScopeResolver;
import com.msm.core.security.RequestContextHolder;
import com.msm.core.security.SecurityCheckProvider;
import com.msm.core.security.context.AuthorizationContext;
import com.msm.core.security.context.RequestContext;
import com.msm.core.security.enums.PermissionAction;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PermissionService {
    private final DSLContext dsl;
    private final SecurityCheckProvider securityCheckProvider;

    public boolean canView(String objectName) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        return authorizationContext.canView(resolveObjectAccessScope(objectName));
    }

//    public boolean canCreate(String objectName) {
//        RequestContext requestContext = RequestContextHolder.getRequestContext();
//        AuthorizationContext authorizationContext = requestContext.getAuthorization();
//        String securityTargetMetadataName = resolveObjectAccessScope(objectName);
//
//
//        boolean canCreate = authorizationContext.canCreate(resolveObjectAccessScope(objectName));
//        if (canCreate) {
//
//
//            Condition securityCondition = SecurityConditionBuilder.buildCreateDataScopeCondition(
//                    ObjectMetadataFactory.getObjectMetadataByName(securityTargetMetadataName),
//                    dataScopeResolver
//            );
//
//
//            SecurityConditionBuilder.checkDataScope(objectName, dataScopeResolver, )
//
//        }
//    }

    public boolean canCreate(String objectName, Map<String, Object> payload) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
//        String securityTargetMetadataName = resolveObjectAccessScope(objectName);


        boolean canCreate = authorizationContext.canCreate(resolveObjectAccessScope(objectName));
        if (canCreate) {
            return securityCheckProvider.checkDataScope(objectName, payload, PermissionAction.CREATE);
        }

        return false;
    }

    public boolean canBulkCreate(String objectName, List<Map<String, Object>> payload) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();

        boolean canCreate = authorizationContext.canCreate(resolveObjectAccessScope(objectName));
        if (canCreate) {
            return securityCheckProvider.checkDataScope(objectName, payload, PermissionAction.CREATE);
        }

        return false;
    }

    public boolean canEdit(String objectName, Map<String, Object> payload) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        boolean canEdit = authorizationContext.canUpdate(resolveObjectAccessScope(objectName));

        if (canEdit) {
            return securityCheckProvider.checkDataScope(objectName, payload, PermissionAction.UPDATE);
        }

        return false;
    }

    public boolean canBulkEdit(String objectName, List<Map<String, Object>> payloads) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        boolean canEdit = authorizationContext.canUpdate(resolveObjectAccessScope(objectName));

        if (canEdit) {
            return securityCheckProvider.checkDataScope(objectName, payloads, PermissionAction.UPDATE);
        }

        return false;
    }

    public boolean canDelete(String objectName, UUID id, Long version) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        boolean canDelete = authorizationContext.canDelete(resolveObjectAccessScope(objectName));

        if (canDelete) {
            ObjectMetadata securityObjectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
            Set<String> returnFields = securityObjectMetadata
                    .getSecuredAttributes()
                    .values()
                    .stream()
                    .map(Attribute::getFieldName)
                    .collect(Collectors.toSet());
            if(securityObjectMetadata.getObjectRelation() != null) {
                returnFields.add(securityObjectMetadata.getObjectRelation().getForeignKeyAttribute());
            }
            Field<Object> fieldId = (Field<Object>) securityObjectMetadata.getIdAttribute().getField();
            SelectConditionStep<Record> query = dsl
                    .select(SelectBuilder.buildFields(securityObjectMetadata, new ArrayList<>(returnFields)))
                    .from(securityObjectMetadata.getTable())
                    .where(fieldId.eq(id));
            Map<String, Object> payload = query.fetchOneMap();
            return securityCheckProvider.checkDataScope(objectName, payload, PermissionAction.DELETE);
        }

        return false;
    }

    public boolean canBulkDelete(String objectName, List<ObjectDeleteRequest> objectDeleteRequests) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        boolean canDelete = authorizationContext.canDelete(resolveObjectAccessScope(objectName));

        if (canDelete) {
            ObjectMetadata securityObjectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
            Set<String> returnFields = securityObjectMetadata
                    .getSecuredAttributes()
                    .values()
                    .stream()
                    .map(Attribute::getFieldName)
                    .collect(Collectors.toSet());
            if(securityObjectMetadata.getObjectRelation() != null) {
                returnFields.add(securityObjectMetadata.getObjectRelation().getForeignKeyAttribute());
            }
            Field<Object> fieldId = (Field<Object>) securityObjectMetadata.getIdAttribute().getField();
            Set<UUID> deleteIds = objectDeleteRequests.stream().map(ObjectDeleteRequest::getId).collect(Collectors.toSet());
            SelectConditionStep<Record> query = dsl
                    .select(SelectBuilder.buildFields(securityObjectMetadata, new ArrayList<>(returnFields)))
                    .from(securityObjectMetadata.getTable())
                    .where(fieldId.in(deleteIds));
            List<Map<String, Object>> payloads = query.fetchMaps();

            return securityCheckProvider.checkDataScope(objectName, payloads, PermissionAction.DELETE);
        }

        return false;
    }

    public boolean canProcessAction(String objectName) {
        RequestContext requestContext = RequestContextHolder.getRequestContext();
        AuthorizationContext authorizationContext = requestContext.getAuthorization();
        return authorizationContext.canCreate(resolveObjectAccessScope(objectName));
    }

    private String resolveObjectAccessScope(String objectName) {
        return ObjectAccessScopeResolver.resolveObjectAccessScope(objectName);
    }
}

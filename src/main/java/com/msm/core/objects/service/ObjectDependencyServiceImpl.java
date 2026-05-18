package com.msm.core.objects.service;

import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.AttributeRef;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.connector.MasterDataApiService;
import com.msm.core.objects.dto.metadata.ObjectDependencyMeta;
import com.msm.core.objects.handler.GenericObjectHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ObjectDependencyServiceImpl implements ObjectDependencyService {
    //outbox
    private static final String OUTBOX_TABLE_NAME = "outbox";
    private static final String OBJECT_DEPENDENCY_CONSTRAINT_NAME = "object_dependency_uq_dependency";
    private final GenericObjectHandler genericObjectHandler;
    private final MasterDataApiService masterDataApiService;

    public void saveObjectDependency(ActionContext<Map<String, Object>> actionContext) {
//        Optional<ObjectMetadata> outboxMetadataOptional = ObjectMetadataFactory.getObjectMetadata(OUTBOX_TABLE_NAME);
//        if(outboxMetadataOptional.isPresent()){
//            ObjectMetadata outboxMetadata = outboxMetadataOptional.get();
//
//            Map<String, Object> valueMap = (Map<String, Object>) actionContext.getResult();
//            Optional<ObjectMetadata> objectMetadataOptional = ObjectMetadataFactory.getObjectMetadata(actionContext.getResource());
//
//            if(objectMetadataOptional.isPresent()){
//                ObjectMetadata  objectMetadata = objectMetadataOptional.get();
//                List<Map<String, Object>> dependencyPayload = buildObjectDependencyMap(objectMetadata, valueMap);
//
//                DataRecord outboxRecord = DataRecord.of(new HashMap<>());
//                outboxRecord.set(OutboxEntityMeta.AGGREGATE_TYPE, actionContext.getResource());
//                outboxRecord.set(OutboxEntityMeta.AGGREGATE_ID, UUID.fromString(String.valueOf(valueMap.get(objectMetadata.getIdAttribute().getFieldName()))));
//                //OBJECT_DEPENDENCY
//                outboxRecord.set(OutboxEntityMeta.AGGREGATE_SUBTYPE, "OBJECT_DEPENDENCY");
//                //destination
//                outboxRecord.set(OutboxEntityMeta.DESTINATION, "MASTER_DATA");
//
//                try {
//                    outboxRecord.set(OutboxEntityMeta.PAYLOAD, JSONB.jsonb(Utils.O.toJsonString(dependencyPayload)));
//                } catch (JsonProcessingException e) {
//                    throw ObjectErrors.payloadInvalidException(actionContext.getResource(), e.getOriginalMessage(), e);
//                }
//            }
//
//        }

    }

    public void sendEvent(ActionContext<Map<String, Object>> actionContext) {
        Optional<ObjectMetadata> objectMetadataOptional = ObjectMetadataFactory.getObjectMetadata(actionContext.getResource());
        Map<String, Object> valueMap = (Map<String, Object>) actionContext.getResult();
        List<Map<String, Object>> dependencyPayload = buildObjectDependencyMap(objectMetadataOptional.get(), valueMap);
        boolean isInternal = isInternalObjectDependency();
        if(isInternal && Utils.CL.isNotEmpty(dependencyPayload)) {
            genericObjectHandler.bulkCreateIgnoreConflictOnConstraintName(ObjectDependencyMeta.OBJECT_NAME, dependencyPayload, OBJECT_DEPENDENCY_CONSTRAINT_NAME);
        } else if(Utils.CL.isNotEmpty(dependencyPayload)){
            masterDataApiService.post(dependencyPayload);
        }
    }

    private List<Map<String, Object>> buildObjectDependencyMap(ObjectMetadata objectMetadata, Map<String, Object> objectValues){
        List<Map<String, Object>> objectDependencyMap = new LinkedList<>();
        List<Attribute> attributeRefs = objectMetadata.getAttributeRefs();
        String sourceType = objectMetadata.getName();
        UUID sourceId = UUID.fromString(String.valueOf(objectValues.get(objectMetadata.getIdAttribute().getFieldName())));
        attributeRefs.forEach(attribute -> {
            AttributeRef ref = attribute.getAttributeRef();
            Object objectTargetId = objectValues.get(attribute.getFieldName());
            if(objectTargetId != null) {
                UUID targetId = UUID.fromString(String.valueOf(objectTargetId));
                DataRecord objectDependencyRecord = DataRecord.of(new HashMap<>());
                objectDependencyRecord.set(ObjectDependencyMeta.SOURCE_TYPE, sourceType);
                objectDependencyRecord.set(ObjectDependencyMeta.SOURCE_ID, sourceId);
                objectDependencyRecord.set(ObjectDependencyMeta.TARGET_TYPE, ref.getObjectRef());
                objectDependencyRecord.set(ObjectDependencyMeta.TARGET_ID, targetId);
                objectDependencyRecord.set(ObjectDependencyMeta.DEPENDENCY_TYPE, ref.getUsageType());
                objectDependencyRecord.set(ObjectDependencyMeta.DEPENDENCY_FIELD, attribute.getFieldName());
                objectDependencyMap.add(objectDependencyRecord.getValues());
            }
        });

        return objectDependencyMap;
    }

    private boolean isInternalObjectDependency() {
        return ObjectMetadataFactory.getObjectMetadata(ObjectDependencyMeta.OBJECT_NAME).isPresent();
    }
}

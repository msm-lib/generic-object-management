package com.msm.core.objects.imports.reference;

import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterObject;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.AttributeRef;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.imports.model.AttributeLookup;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TypeAndCodeReferenceResolver {
    private static final String CODE = "code";
    private static final String GEOGRAPHY_TYPE_ID = "geographyTypeId";

    protected final GenericObjectInternalService genericObjectInternalService;
    protected final ObjectQueryRepository internalObjectQueryRepository;



    private Condition buildCondition(ObjectMetadata objectMetadata, List<AttributeLookup> attributeLookups, Map<String, Set<String>> lookupValues) {
        Condition condition = DSL.noCondition();
        for (AttributeLookup attributeLookup : attributeLookups) {
            Set<String> lookupVals = lookupValues.get(attributeLookup.attributeName());
            if (Utils.CL.size(lookupVals) > 0) {
                Attribute attr = objectMetadata.getAttributeByName(attributeLookup.attributeName());
                Field<Object> typeField = (Field<Object>) attr.getField();
                condition = condition.and(typeField.in(lookupVals));
            }
        }

        return condition;
    }

    private List<FilterObject> buildFilterCondition(List<AttributeLookup> attributeLookups, Map<String, Set<String>> lookupValues) {
        return attributeLookups.stream()
                .map(e -> FilterCondition.create(e.attributeName(), FilterOperator.IN, lookupValues.get(e.attributeName())))
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, Map<String, Object>>> resolve(
            String sourceObjectName,
            List<AttributeLookup> attributeLookups,
            Attribute sourceAttribute,//continentId
            List<Map<String, Object>> items) {

//        Set<String> codes = AttributeRefHelper.getCodes(attribute, items);
        Map<String, Set<String>> lookupValues = AttributeRefHelper.getLookupValueMap(sourceAttribute, attributeLookups, items);

        AttributeRef attributeRef = sourceAttribute.getAttributeRef();
        String targetObjectName = attributeRef.getObjectRef();
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(targetObjectName);

        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            ObjectMetadata objectMetadata = optionalObjectMetadata.get();
//            Attribute codeAttr = objectMetadata.getAttributeByName(CODE);
//            Attribute typeAttr = objectMetadata.getAttributeByName(GEOGRAPHY_TYPE_ID);
//            Field<Object> typeIdField = (Field<Object>) typeAttr.getField();
            objectList = internalObjectQueryRepository.findByCondition(
                    targetObjectName,
                    buildCondition(objectMetadata, attributeLookups, lookupValues),
                    AttributeRefHelper.getOrDefaultReturnFields(attributeRef)
            );
        } else {
            String objectRefName = sourceAttribute.getAttributeRef().getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .returnFields(AttributeRefHelper.getOrDefaultReturnFields(attributeRef))
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(
                            buildFilterCondition(attributeLookups, lookupValues)
                    ).build())
                    .build();


            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = result.getContents();
        }

        AttributeRefHelper.retainAllRefData(sourceAttribute, objectList);
        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(
                Utils.CL.emptyIfNull(objectList),
                objectKey -> String.valueOf(objectKey.get(getDefaultKey(attributeLookups))),
                objectValue -> objectValue);
        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(sourceAttribute.getFieldName(), codeMap);

        return objectMap;
    }

    private String getDefaultKey(List<AttributeLookup> attributeLookups) {
        Optional<AttributeLookup> attributeLookup = attributeLookups.stream().filter(AttributeLookup::isKey).findFirst();
        if (attributeLookup.isPresent()) {
            return attributeLookup.get().attributeName();
        }
        return CODE;
    }

}

package com.msm.core.objects.dataexchange.imports.reference;

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
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.dataexchange.imports.ImportConfigService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.utils.JsonPathUtil;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AttributeReferenceResolver {
    private static final String CODE = "code";
    protected final GenericObjectInternalService genericObjectInternalService;
    protected final ObjectQueryRepository internalObjectQueryRepository;
    protected final ImportConfigService importConfigService;



    private Condition buildCondition(ObjectMetadata objectMetadata, List<ObjectImportRegistry.LookupValuesConfig> attributeLookups, Map<String, Set<String>> lookupValues) {
        Condition condition = DSL.noCondition();
        for (ObjectImportRegistry.LookupValuesConfig attributeLookup : attributeLookups) {
            Set<String> lookupVals = lookupValues.get(attributeLookup.getAttributeName());
            if (Utils.CL.size(lookupVals) > 0) {
                Attribute attr = objectMetadata.getAttributeByName(attributeLookup.getAttributeName());
                Field<Object> typeField = (Field<Object>) attr.getField();
                condition = condition.and(typeField.in(lookupVals));
            }
        }

        return condition;
    }

    private List<FilterObject> buildFilterCondition(List<ObjectImportRegistry.LookupValuesConfig> attributeLookups, Map<String, Set<String>> lookupValues) {
        return attributeLookups.stream()
                .map(e -> FilterCondition.create(e.getFilterAttribute(), FilterOperator.IN, lookupValues.get(e.getAttributeName())))
                .collect(Collectors.toList());
    }

    private boolean isEmpty(Map<String, Set<String>> lookupValues) {
        return lookupValues.values().stream()
                .anyMatch(set -> set == null || set.isEmpty());
    }

    public Map<String, Map<String, Map<String, Object>>> resolve(
            String sourceObjectName,
            Attribute sourceAttribute,//continentId
            List<Map<String, Object>> items) {

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = importConfigService
                .getReferenceConfig(sourceObjectName, sourceAttribute.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();

        Map<String, Set<String>> lookupValues = AttributeRefHelper.getLookupValueMap(sourceAttribute, attributeLookups, items);

        if(isEmpty(lookupValues)) {
            return Map.of();
        }

        AttributeRef attributeRef = sourceAttribute.getAttributeRef();
        String targetObjectName = attributeRef.getObjectRef();
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(targetObjectName);

        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            ObjectMetadata objectMetadata = optionalObjectMetadata.get();
            objectList = internalObjectQueryRepository.findByCondition(
                    targetObjectName,
                    buildCondition(objectMetadata, attributeLookups, lookupValues),
                    referenceDetailConfig.getFields()
            );
        } else {
            String objectRefName = sourceAttribute.getAttributeRef().getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .returnFields(referenceDetailConfig.getFields())
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(
                            buildFilterCondition(attributeLookups, lookupValues)
                    ).build())
                    .build();


            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = result.getContents();
        }

        objectList = objectList.stream().map(objectMap -> getRefMappedData(
                sourceObjectName,
                sourceAttribute.getAttributeRef().getFieldName(),
                objectMap
        )).collect(Collectors.toList());

        AttributeRefHelper.retainAllRefData(getMappingFieldValues(referenceDetailConfig), objectList);
        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(
                Utils.CL.emptyIfNull(objectList),
                objectKey -> String.valueOf(objectKey.get(getDefaultKey(attributeLookups))),
                objectValue -> objectValue);
        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(sourceAttribute.getFieldName(), codeMap);

        return objectMap;
    }

    private String getDefaultKey(List<ObjectImportRegistry.LookupValuesConfig> attributeLookups) {
        Optional<ObjectImportRegistry.LookupValuesConfig> attributeLookup = attributeLookups
                .stream()
                .filter(ObjectImportRegistry.LookupValuesConfig::isPrimary)
                .findFirst();
        if (attributeLookup.isPresent()) {
            return attributeLookup.get().getAttributeName();
        }
        return CODE;
    }

    private Set<String> getMappingFieldValues(ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig) {
        if (Utils.CL.isNotEmpty(referenceDetailConfig.getMappingValues())) {
            return referenceDetailConfig.getMappingValues().keySet();
        }
        return new HashSet<>(referenceDetailConfig.getFields());
    }


    private Map<String, Object> getRefMappedData(String importObjectName, String attrRefName, Map<String, Object> redData) {
        Map<String, String> refMappingConfig = importConfigService.getReferenceConfig(importObjectName, attrRefName).getMappingValues();

        if (Utils.CL.isEmpty(refMappingConfig)) {
            return redData;
        }

        return refMappingConfig
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> JsonPathUtil.extractValue(redData, entry.getValue()),
                        (existingValue, newValue) -> newValue,
                        HashMap::new
                ));
    }
}

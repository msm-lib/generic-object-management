package com.msm.core.objects.service.imports.resolver.impl;

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
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.imports.resolver.strategy.ReferenceResolver;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class GeographyTypeCodeLookup implements ReferenceResolver {

    private final String CODE = "code";
    private final String GEOGRAPHY_TYPE_ID = "geographyTypeId";
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final GenericObjectInternalService genericObjectInternalService;

    @Override
    public String sourceObject() {
        return "geographylocation";
    }

    @Override
    public String targetObject() {
        return "geographylocation";
    }

    @Override
    public String sourceAttribute() {
        return "parentId";
    }

    public Map<String, Map<String, Map<String, Object>>> resolve(
            String sourceObjectName,
            Attribute attribute,
            List<Map<String, Object>> items) {

        Set<String> codes = new HashSet<>();
        Set<UUID> parentGeographyTypes = new HashSet<>();
        items.forEach(item -> {
            Object parentCode = item.get("parentId");
            if(Objects.nonNull(parentCode) && Utils.STR.isNotBlank(parentCode.toString())) {
                codes.add(parentCode.toString());
            }
            Object geographyTypeIdObj = item.get("parentGeographyTypeId");
            if(Objects.nonNull(geographyTypeIdObj) && Utils.STR.isNotBlank(geographyTypeIdObj.toString())) {
                parentGeographyTypes.add(UUID.fromString(geographyTypeIdObj.toString()));
            }
        });

        String targetObjectName = attribute.getAttributeRef().getObjectRef();
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(targetObjectName);

        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            ObjectMetadata objectMetadata = optionalObjectMetadata.get();
            Attribute codeAttr = objectMetadata.getAttributeByName("code");
            Attribute geographyTypeAttr = objectMetadata.getAttributeByName("geographyTypeId");
            objectList = internalObjectQueryRepository.findByCondition(
                    targetObjectName,
                    codeAttr.getField().in(codes).and(geographyTypeAttr.getField().in(parentGeographyTypes)),
                    DEFAULT_RETURN_FIELDS
            );
        } else {
            String objectRefName = attribute.getAttributeRef().getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .returnFields(DEFAULT_RETURN_FIELDS)
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(
                            Utils.CL.newArrayList(
                                    new FilterObject[]{
                                            FilterCondition.create(CODE, FilterOperator.IN, codes),
                                            FilterCondition.create(GEOGRAPHY_TYPE_ID, FilterOperator.IN, parentGeographyTypes)
                                    }
                            )
                    ).build())
                    .build();


            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = result.getContents();
        }

        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(Utils.CL.emptyIfNull(objectList), objectKey -> String.valueOf(objectKey.get(CODE)), objectValue -> objectValue);
        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(attribute.getFieldName(), codeMap);

        return objectMap;
    }
}

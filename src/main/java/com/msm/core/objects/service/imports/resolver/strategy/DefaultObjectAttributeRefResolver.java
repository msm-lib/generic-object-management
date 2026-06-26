package com.msm.core.objects.service.imports.resolver.strategy;

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
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DefaultObjectAttributeRefResolver implements ReferenceResolver {

    private final String ATTRIBUTE_LOOKUP_NAME = "code";
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final GenericObjectInternalService genericObjectInternalService;

    @Override
    public String object() {
        return "*";
    }

    @Override
    public String attribute() {
        return "*";
    }


    @Override
    public Map<String, Map<String, Map<String, Object>>> resolve(String objectName, Attribute attribute, List<Map<String, Object>> items) {
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(objectName);

        List<String> codes = items.stream().map(objectValue -> {
            Object valCode = objectValue.get(attribute.getFieldName());
            if(Objects.nonNull(valCode)) {
                return valCode.toString();
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            objectList = internalObjectQueryRepository.findAllByCondition(objectName, DSL.field(ATTRIBUTE_LOOKUP_NAME).in(codes), returnFields());
        } else {
            String objectRefName = attribute.getAttributeRef().getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .returnFields(returnFields())
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(Utils.CL.newArrayList(new FilterObject[]{FilterCondition.create(ATTRIBUTE_LOOKUP_NAME, FilterOperator.IN, codes)})).build())
                    .build();

            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = result.getContents();
        }

        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(Utils.CL.emptyIfNull(objectList), objectKey -> String.valueOf(objectKey.get(ATTRIBUTE_LOOKUP_NAME)), objectValue -> objectValue);
        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(attribute.getFieldName(), codeMap);

        return objectMap;
    }


}

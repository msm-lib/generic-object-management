package com.msm.core.objects.service.imports.resolver;

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
import com.msm.core.objects.handler.GenericObjectHandler;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class ObjectAttributeRefResolver implements ReferenceResolver {

    private final String ATTRIBUTE_QUERY_NAME = "code";
    private final GenericObjectHandler genericObjectHandler;
    private final GenericObjectInternalService genericObjectInternalService;

    @Override
    public String support() {
        return "ReferenceByCode";
    }

    @Override
    public Map<String, Map<String, Map<String, Object>>> resolve(String objectName, Attribute attribute, List<String> codes) {
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(objectName);
        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            objectList = genericObjectHandler.findAllByCondition(objectName, DSL.field(ATTRIBUTE_QUERY_NAME).in(codes));
        } else {
            String objectRefName = attribute.getAttributeRef().getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .returnFields(null)
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(Utils.CL.newArrayList(new FilterObject[]{FilterCondition.create(ATTRIBUTE_QUERY_NAME, FilterOperator.IN, codes)})).build())
                    .build();


            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = result.getContents();
        }

        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(Utils.CL.emptyIfNull(objectList), objectKey -> String.valueOf(objectKey.get(ATTRIBUTE_QUERY_NAME)), objectValue -> objectValue);
        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(attribute.getFieldName(), codeMap);

        return objectMap;
    }


}

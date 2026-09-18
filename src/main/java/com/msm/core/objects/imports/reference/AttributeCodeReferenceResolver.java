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
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.imports.ImportConfigService;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class AttributeCodeReferenceResolver {
    private static final String ATTRIBUTE_LOOKUP_NAME = "code";
    protected final GenericObjectInternalService genericObjectInternalService;
    protected final ObjectQueryRepository internalObjectQueryRepository;
    protected final ImportConfigService importConfigService;


    public Map<String, Map<String, Map<String, Object>>> resolve(
            String sourceObjectName,
            Attribute attribute,
            List<Map<String, Object>> items
    ) {
        AttributeRef attributeRef = attribute.getAttributeRef();
        String targetObjectName = attributeRef.getObjectRef();
        Optional<ObjectMetadata> optionalObjectMetadata = ObjectMetadataFactory.getObjectMetadata(targetObjectName);

        Set<String> codes = AttributeRefHelper.getCodes(attribute, items);
        if(Utils.CL.isEmpty(codes)) return new HashMap<>();

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = importConfigService.getReferenceConfig(sourceObjectName, attribute.getAttributeRef().getFieldName());

        List<Map<String, Object>> objectList;
        if(optionalObjectMetadata.isPresent()) {
            objectList = Utils.CL.emptyIfNull(
                    internalObjectQueryRepository.findByCondition(
                            targetObjectName,
                            DSL.field(ATTRIBUTE_LOOKUP_NAME).in(codes),
                            referenceDetailConfig.getFields()
                    )
            );
        } else {
            String objectRefName = attributeRef.getObjectRef();
            ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                    .builder()
                    .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectRefName))
                    .filters(FilterGroup.builder().operator(LogicalOperator.AND).conditions(Utils.CL.newArrayList(new FilterObject[]{FilterCondition.create(ATTRIBUTE_LOOKUP_NAME, FilterOperator.IN, codes)})).build())
                    .returnFields(referenceDetailConfig.getFields())
                    .build();

            PageResponse<Map<String, Object>> result = genericObjectInternalService.filter(objectRefName, objectFilterRequest);
            objectList = Utils.CL.emptyIfNull(result.getContents());
        }

        AttributeRefHelper.retainAllRefData(referenceDetailConfig.getFields(), objectList);
        Map<String, Map<String, Object>> codeMap = Utils.CL.toMap(
                objectList,
                objectKey -> String.valueOf(objectKey.get(ATTRIBUTE_LOOKUP_NAME)),
                objectValue -> objectValue);

        Map<String, Map<String, Map<String, Object>>> objectMap = new HashMap<>();
        objectMap.put(attribute.getFieldName(), codeMap);

        return objectMap;
    }

//    protected void retainAllRefData(Attribute attribute, List<Map<String, Object>> objectList) {
//        Set<String> refNames = (Set<String>) Utils.CL.defaultIfEmpty(attribute.getAttributeRef().getAttributeRefs(), AttributeRefHelper.getOrDefaultReturnFields(attribute.getAttributeRef()));
//        objectList.forEach(objectValue -> {
//            objectValue.keySet().retainAll(refNames);
//        });
//    }
}

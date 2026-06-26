package com.msm.core.objects.service.imports;


import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.ValidateAndPopulateDataService;
import com.msm.core.objects.service.imports.resolver.Resolver;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class BatchExecutionService {

    private final ValidateAndPopulateDataService validateAndPopulateDataService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final Resolver resolve;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void batchImportAndCleanup(String objectName, List<Map<String, Object>> items) {

        fillRefData(objectName, items);

//        attrCodeMap.forEach((attribute, codes) -> {
//            //attribute -> {code -> {id, code, name}}
//            Map<String, Map<String, Map<String, Object>>> refMapList;
//            if(attribute.getFieldName().equalsIgnoreCase("parentId")) {
//                refMapList = geographyTypeCodeLookup.resolve(attribute.getAttributeRef().getObjectRef(),  attribute, items);
//            } else {
//                refMapList = referenceResolver.resolve(attribute.getAttributeRef().getObjectRef(), attribute, items);
//            }
//            items.forEach(itemMap -> {
//                String attrName = attribute.getFieldName();
//                Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
//                if(Utils.CL.isNotEmpty(objectCodeMap)) {
//                    String codeRef = String.valueOf(itemMap.get(attrName));
//                    Map<String, Object> objectRef = objectCodeMap.get(codeRef);
//                    if(objectRef != null) {
//                        Object idObj = objectRef.get(Constants.OBJECT_PK);
//                        itemMap.put(attrName, idObj);
//                        itemMap.put(Utils.STR.format(Constants.ATTRIBUTE_REF_TEMPLATE, attrName), objectRef);
//                    }
//                }
//            });
//        });

        //account_code_key
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);

        validateAndPopulateDataService.validate(objectMetadata, items);
        internalObjectQueryRepository.bulkUpsertReturning(
                objectName,
                items,
                List.of("code", "geographyTypeId"));
        items.clear();
//        attrCodeMap.clear();
    }

    private void fillRefData(String objectName, List<Map<String, Object>> items) {

        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
        objectMetadata.getAttributes().forEach(attr -> {
            if (hasRef(attr)) {
                Map<String, Map<String, Map<String, Object>>> refMapList = resolve.resolve(objectName, attr, items);
                items.forEach(itemMap -> {
                    String attrName = attr.getFieldName();
                    Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
                    if(Utils.CL.isNotEmpty(objectCodeMap)) {
                        String codeRef = String.valueOf(itemMap.get(attrName));
                        Map<String, Object> objectRef = objectCodeMap.get(codeRef);
                        if(objectRef != null) {
                            Object idObj = objectRef.get(Constants.OBJECT_PK);
                            itemMap.put(attrName, idObj);
                            itemMap.put(Utils.STR.format(Constants.ATTRIBUTE_REF_TEMPLATE, attrName), objectRef);
                        }
                    }
                });
            }
        });
    }

    private boolean hasRef(Attribute attribute) {
        return Objects.nonNull(attribute.getAttributeRef()) && Utils.STR.isNotBlank(attribute.getAttributeRef().getFieldName());
    }
}

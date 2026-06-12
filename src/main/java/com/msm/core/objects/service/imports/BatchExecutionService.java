package com.msm.core.objects.service.imports;


import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.handler.GenericObjectHandler;
import com.msm.core.objects.service.ValidateAndPopulateDataService;
import com.msm.core.objects.service.imports.resolver.ReferenceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class BatchExecutionService {

    private final ReferenceResolver referenceResolver;
    private final ValidateAndPopulateDataService validateAndPopulateDataService;
    private final GenericObjectHandler genericObjectHandler;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void batchImportAndCleanup(String objectName, List<Map<String, Object>> items, Map<Attribute, Set<String>> attrCodeMap) {
        attrCodeMap.forEach((attribute, codes) -> {
            //attribute -> {code -> {id, code, name}}
            Map<String, Map<String, Map<String, Object>>> refMapList = referenceResolver.resolve(attribute.getAttributeRef().getObjectRef(), attribute, new ArrayList<>(codes));
            items.forEach(itemMap -> {
                String attrName = attribute.getFieldName();
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
        });

        //account_code_key
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);

        validateAndPopulateDataService.validate(objectMetadata, items);
//        genericObjectHandler.bulkUpsertReturning(
//                objectName,
//                items,
//                Utils.STR.format(Constants.CONSTRAINT_KEY, objectMetadata.getTableName(), "code", "key"));
        genericObjectHandler.bulkUpsertReturning(
                objectName,
                items,
                null);
        items.clear();
        attrCodeMap.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void batchImport(String objectName, List<Map<String, Object>> items, Map<Attribute, Set<String>> attrCodeMap) {
        attrCodeMap.forEach((attribute, codes) -> {
            //attribute -> {code -> {id, code, name}}
            Map<String, Map<String, Map<String, Object>>> refMapList = referenceResolver.resolve(attribute.getAttributeRef().getObjectRef(), attribute, new ArrayList<>(codes));
            items.forEach(itemMap -> {
                String attrName = attribute.getFieldName();
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
        });

    }
}

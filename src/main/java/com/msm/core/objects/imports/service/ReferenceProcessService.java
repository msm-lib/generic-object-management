package com.msm.core.objects.imports.service;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import com.msm.core.objects.imports.model.ImportRow;
import com.msm.core.objects.imports.reference.AttributeRefHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ReferenceProcessService {
    private final ActionExecutor actionExecutor;


    public void batchRefProcessing(UUID importId, String importObjectName, List<ImportRow> rows) {
        List<Map<String, Object>> items = rows.stream().map(ImportRow::rowData).collect(Collectors.toList());
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(importObjectName);
        objectMetadata.getAttributes().forEach(attr -> {
            if (AttributeRefHelper.hasRef(attr)) {
                Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, importObjectName, attr, items);
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

//    public void fillReferenceData(UUID importId, String objectName, List<Map<String, Object>> items) {
//        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(objectName);
//        objectMetadata.getAttributes().forEach(attr -> {
//            if (AttributeRefHelper.hasRef(attr)) {
//                Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, objectName, attr, items);
//                items.forEach(itemMap -> {
//                    String attrName = attr.getFieldName();
//                    Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
//                    if(Utils.CL.isNotEmpty(objectCodeMap)) {
//                        String codeRef = String.valueOf(itemMap.get(attrName));
//                        Map<String, Object> objectRef = objectCodeMap.get(codeRef);
//                        if(objectRef != null) {
//                            Object idObj = objectRef.get(Constants.OBJECT_PK);
//                            itemMap.put(attrName, idObj);
//                            itemMap.put(Utils.STR.format(Constants.ATTRIBUTE_REF_TEMPLATE, attrName), objectRef);
//                        }
//                    }
//                });
//            }
//        });
//    }

    private Map<String, Map<String, Map<String, Object>>> resolveAttributeRef(
            UUID importId,
            String importObjectName,
            Attribute attribute,
            List<Map<String, Object>> items) {

        try {
            String objectCellResource = Utils.STR.format("{0}.{1}",  importObjectName, attribute.getFieldName());
            AttributeReferenceContext attributeReferenceContext = AttributeReferenceContext.of(importId,  importObjectName, attribute, items);

            ActionContext<AttributeReferenceContext> actionContext = ActionContext
                    .<AttributeReferenceContext>builder()
                    .resource(objectCellResource)
                    .action(ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
                    .payload(attributeReferenceContext)
                    .build();

            return actionExecutor.execute(actionContext);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Map.of();
    }
}

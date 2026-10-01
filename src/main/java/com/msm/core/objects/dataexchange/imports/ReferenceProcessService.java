package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.keys.IdentityKeyGenerator;
import com.msm.core.objects.dataexchange.imports.model.AttributeReferenceContext;
import com.msm.core.objects.dataexchange.imports.model.AttributeReferenceFailed;
import com.msm.core.objects.dataexchange.imports.model.IdentityKey;
import com.msm.core.objects.dataexchange.imports.model.ImportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ReferenceProcessService {
    private final ActionExecutor actionExecutor;
    protected final ImportConfigService importConfigService;

//    public void batchRefProcessing(UUID importId, String importObjectName, List<ImportRow> rows) {
//        List<Map<String, Object>> items = rows.stream().map(ImportRow::rowData).collect(Collectors.toList());
//        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(importObjectName);
//        objectMetadata.getAttributes().forEach(attr -> {
//            if (AttributeRefHelper.hasRef(attr)) {
//                Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, importObjectName, attr, items);
//                items.forEach(itemMap -> {
//                    String attrName = attr.getFieldName();
//                    Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
//                    if(Utils.CL.isNotEmpty(objectCodeMap)) {
//                        IdentityKey identityKey = IdentityKeyGenerator.generateKey(itemMap, importConfigService.getSourceFieldNames(importObjectName, attr));
//                        if(identityKey != null) {
//                            String keyCodeRef = identityKey.key();
//                            Map<String, Object> objectRef = objectCodeMap.get(keyCodeRef);
//                            if(objectRef != null) {
//                                Object idObj = objectRef.get(Constants.OBJECT_PK);
//                                itemMap.put(attrName, idObj);
//                                itemMap.put(attr.getAttributeRef().getFieldName(), objectRef);
//                            }
//                        }
//                    }
//                });
//            }
//        });
//    }




    public void batchRefProcessing(UUID jobId, String importObjectName, List<ImportRow> rows) {
        ObjectMetadata objectMetadata = ObjectMetadataFactory.getObjectMetadataByName(importObjectName);

        //Find all attribute reference
        List<Attribute> refs = objectMetadata.getAttributeRefs();
        //Group by field name
        Map<String, Attribute> attributeReferenceMap = Utils.D.groupBy(
                refs,
                Attribute::getFieldName,
                Function.identity()
        );

        Set<AttributeReferenceFailed> attributeReferenceFailed = new HashSet<>();
        for (Attribute ref : refs) {
            if(attributeReferenceMap.containsKey(ref.getFieldName())) {
                resolveAttributeRefDependency(jobId, objectMetadata, ref, attributeReferenceMap, rows, attributeReferenceFailed);
            }
        }
    }

    public void resolveAttributeRefDependency(UUID importId, ObjectMetadata objectMetadata, Attribute attr, Map<String, Attribute> attributeReferenceMap, List<ImportRow> rows, Set<AttributeReferenceFailed> attributeReferenceFailed) {

        //Get dependency attribute if exists
        List<ObjectImportRegistry.LookupValuesConfig> dependenciesRef = getDependencies(objectMetadata, attr, attributeReferenceMap);
        if(Utils.CL.isNotEmpty(dependenciesRef)) {
            dependenciesRef.forEach(lookupValuesConfig -> {
                Attribute attributeDependency = objectMetadata.getAttributeByName(lookupValuesConfig.getSourceField());
                resolveAttributeRefDependency(importId, objectMetadata, attributeDependency, attributeReferenceMap, rows, attributeReferenceFailed);
            });
        }

        Map<String, Map<String, Map<String, Object>>> refMapList = resolveAttributeRef(importId, objectMetadata.getName(), attr, rows, attributeReferenceFailed);
        rows.forEach(importRow -> {
            Map<String, Object> itemMap = importRow.rowData();
            String attrName = attr.getFieldName();
            Map<String, Map<String, Object>> objectCodeMap = refMapList.get(attrName);
            if (Utils.CL.isNotEmpty(objectCodeMap)) {
                IdentityKey identityKey = IdentityKeyGenerator.generateKey(itemMap, importConfigService.getSourceFieldNames(objectMetadata.getName(), attr));
                if (identityKey != null) {
                    String keyCodeRef = identityKey.key();
                    Map<String, Object> objectRef = objectCodeMap.get(keyCodeRef);
                    if (objectRef != null) {
                        Object idObj = objectRef.get(Constants.OBJECT_PK);
                        itemMap.put(attrName, idObj);
                        itemMap.put(attr.getAttributeRef().getFieldName(), objectRef);
                    } else {//update attribute failed
                        attributeReferenceFailed.add(AttributeReferenceFailed.of(importRow.rowNumber(), attrName));
                    }
                }
            }
        });

        attributeReferenceMap.remove(attr.getFieldName());
    }

    private List<ObjectImportRegistry.LookupValuesConfig> getDependencies(ObjectMetadata objectMetadata, Attribute attr, Map<String, Attribute> attributeReferenceMap) {
        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = importConfigService
                .getReferenceConfig(objectMetadata.getName(), attr.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();


        return attributeLookups
                .stream()
                .filter(lookupValuesConfig ->  Utils.CL.isEmpty(lookupValuesConfig.getDefaultValues()))
                .filter(lookupValuesConfig -> {
                    String sourceField = lookupValuesConfig.getSourceField();

                    boolean isSameCurrentAttr = sourceField.equals(attr.getFieldName());
                    boolean isDefaultValues = Utils.CL.isNotEmpty(lookupValuesConfig.getDefaultValues());
                    boolean isSourceFieldHRefNotResolved = isSourceFieldHRefNotResolved(objectMetadata, sourceField, attributeReferenceMap);

                    return !isSameCurrentAttr && !isDefaultValues && isSourceFieldHRefNotResolved;
                }).collect(Collectors.toList());

    }

    private boolean isSourceFieldHRefNotResolved(ObjectMetadata objectMetadata, String sourceField, Map<String, Attribute> attributeReferenceMap) {
        boolean sourceFieldHasRef = objectMetadata.hasRef(sourceField);
        if(sourceFieldHasRef) {
            return attributeReferenceMap.containsKey(sourceField);
        }

        return false;
    }

//    private Map<String, Object> getRefMappedData(String importObjectName, String attrName, Map<String, Object> redData) {
//        Map<String, String> refMappingConfig = importConfigService.getReferenceConfig(importObjectName, attrName).getMappingValues();
//
//        if (Utils.CL.isEmpty(refMappingConfig)) {
//            return redData;
//        }
//
//        return refMappingConfig
//                .entrySet()
//                .stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> JsonPathUtil.extractValue(redData, entry.getValue()),
//                        (existingValue, newValue) -> newValue,
//                        HashMap::new
//                ));
//    }



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
            List<ImportRow> rows,
            Set<AttributeReferenceFailed> attributeReferenceFailed
    ) {

        try {
            String objectCellResource = Utils.STR.format("{0}.{1}",  importObjectName, attribute.getFieldName());
            AttributeReferenceContext attributeReferenceContext = AttributeReferenceContext.of(importId,  importObjectName, attribute, rows, attributeReferenceFailed);

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

package com.msm.core.objects.imports;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReferenceResolverService {
//    private final AttributeCodeReferenceResolver attributeCodeReferenceResolver;
//    private final ActionExecutor actionExecutor;
//    private final ObjectQueryRepository internalObjectQueryRepository;
//
//
//    @Handler(action = ImportActionNamed.Csv.REFERENCE_RESOLVE)
//    public void referenceResolver(ActionContext<ReferenceResolveContext> actionContext) {
//        ReferenceResolveContext referenceResolveContext = actionContext.getPayload();
//        fillRefData(
//                referenceResolveContext.importId(),
//                referenceResolveContext.importObjectName(),
//                referenceResolveContext.data()
//        );
//    }
//
//
//
//
//
//    public void fillRefData(UUID importId, String objectName, List<Map<String, Object>> items) {
//
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
//
//    private Map<String, Map<String, Map<String, Object>>> resolveAttributeRef(
//            UUID importId,
//            String importObjectName,
//            Attribute attribute,
//            List<Map<String, Object>> items
//    ) {
//        String objectCellResource = Utils.STR.format("{0}.{1}",  importObjectName, attribute.getFieldName());
//        AttributeReferenceContext attributeReferenceContext = AttributeReferenceContext.of(importId,  importObjectName, attribute, items);
//
//        ActionContext<AttributeReferenceContext> actionContext = ActionContext
//                .<AttributeReferenceContext>builder()
//                .resource(objectCellResource)
//                .action(ImportActionNamed.FIELD_REFERENCE_RESOLVE_PROCESSING)
//                .payload(attributeReferenceContext)
//                .build();
//
//        return actionExecutor.execute(actionContext);
//    }





//
//    private List<Map<String, Object>> getRecordExisted(List<Attribute> attrDuplicates, List<Map<String, Object>> items) {
//        Condition condition = DSL.noCondition();
//        Map<String, Set<Object>> map = getDuplicateValues(attrDuplicates, items);
//        for (Map.Entry<String, Set<Object>> entry : map.entrySet()) {
//            condition = condition.and(DSL.field(entry.getKey()).in(entry.getValue()));
//        }
//
//        return internalObjectQueryRepository.findByCondition("", condition);
//    }
//
//
//    protected Map<String, Set<Object>> getDuplicateValues(List<Attribute> attrDuplicates, List<Map<String, Object>> itemValues) {
//        Set<String> attrNameDuplicates = attrDuplicates.stream().map(Attribute::getFieldName).collect(Collectors.toSet());
//        Map<String, Set<Object>> duplicateMap = new HashMap<>();
//
//        itemValues.forEach(item -> {
//            for(String attr : attrNameDuplicates) {
//                duplicateMap.computeIfAbsent(attr, key -> new HashSet<>()).add(item.get(attr));
//            }
//        });
//
//        return duplicateMap;
//    }


}

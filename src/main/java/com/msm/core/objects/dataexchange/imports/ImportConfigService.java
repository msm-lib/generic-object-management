package com.msm.core.objects.dataexchange.imports;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.objects.config.ObjectImportRegistry;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ImportConfigService {
    private static final ObjectImportRegistry.ObjectConfig DEFAULT_OBJECT_IMPORT_CONFIG = new ObjectImportRegistry.ObjectConfig();
    private static final ObjectImportRegistry.ReferenceDetailConfig DEFAULT_REFERENCE_CONFIG = new ObjectImportRegistry.ReferenceDetailConfig();

    private final ObjectImportRegistry registry;


    public ObjectImportRegistry.ObjectConfig getObject(String objectName) {
        return registry.getObjects().getOrDefault(objectName, DEFAULT_OBJECT_IMPORT_CONFIG);
    }

    public ObjectImportRegistry.ReferenceDetailConfig getReferenceConfig(String objectName, String fieldName) {
        return getObject(objectName).getReferences().getOrDefault(fieldName, DEFAULT_REFERENCE_CONFIG);
    }

    public ObjectImportRegistry.ProcessingConfig getProcessingConfig(String objectName) {
        ObjectImportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getProcessing() == null ? registry.getProcessing() : objectConfig.getProcessing();
    }

    public ObjectImportRegistry.HeaderConfig getHeader(String objectName) {
        ObjectImportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getHeader() == null ? registry.getHeader() : objectConfig.getHeader();
    }

    public List<String> getLookupAttributeNames(String objectName, Attribute attribute) {

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = getReferenceConfig(objectName, attribute.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();
        return attributeLookups
                .stream()
                .filter(lookupValuesConfig -> Utils.CL.isEmpty(lookupValuesConfig.getDefaultValues()))
                .map(ObjectImportRegistry.LookupValuesConfig::getAttributeName)
                .collect(Collectors.toList());
    }

    public List<String> getSourceFieldNames(String objectName, Attribute attribute) {

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = getReferenceConfig(objectName, attribute.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();

        return attributeLookups
                .stream()
                .filter(lookupValuesConfig -> Utils.CL.isEmpty(lookupValuesConfig.getDefaultValues()))
                .map(lookupValuesConfig -> Utils.STR.defaultIfBlank(lookupValuesConfig.getSourceField(), attribute.getFieldName()))
                .collect(Collectors.toList());
    }
}

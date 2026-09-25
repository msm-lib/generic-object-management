package com.msm.core.objects.dataexchange.imports;

import com.msm.core.objects.config.ObjectImportRegistry;
import lombok.RequiredArgsConstructor;

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
}

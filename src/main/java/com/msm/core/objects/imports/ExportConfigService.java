package com.msm.core.objects.imports;

import com.msm.core.commons.Utils;
import com.msm.core.objects.config.ObjectExportRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportConfigService {
    private static final ObjectExportRegistry.ObjectConfig DEFAULT_OBJECT_EXPORT_CONFIG = new ObjectExportRegistry.ObjectConfig();

    private final ObjectExportRegistry registry;


    public ObjectExportRegistry.ObjectConfig getObject(String objectName) {
        return registry.getObjects().getOrDefault(objectName, DEFAULT_OBJECT_EXPORT_CONFIG);
    }
//
//    public ObjectImportRegistry.ReferenceDetailConfig getReferenceConfig(String objectName, String fieldName) {
//        return getObject(objectName).getReferences().getOrDefault(fieldName, DEFAULT_REFERENCE_CONFIG);
//    }

    public String getExportTemplate(String objectName) {
        String template = getObject(objectName).getTemplate();
        return Utils.STR.defaultIfBlank(template, Utils.STR.format(registry.getTemplate(), objectName));
    }

    public ObjectExportRegistry.ProcessingConfig getProcessingConfig(String objectName) {
        ObjectExportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getProcessing() == null ? registry.getProcessing() : objectConfig.getProcessing();
    }

    public ObjectExportRegistry.HeaderConfig getHeader(String objectName) {
        ObjectExportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getHeader() == null ? registry.getHeader() : objectConfig.getHeader();
    }
}

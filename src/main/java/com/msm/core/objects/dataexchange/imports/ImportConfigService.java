package com.msm.core.objects.dataexchange.imports;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.exception.ImportConfigurationException;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ImportConfigService {
    private static final ObjectImportRegistry.ObjectConfig DEFAULT_OBJECT_IMPORT_CONFIG = new ObjectImportRegistry.ObjectConfig();
    private static final ObjectImportRegistry.ReferenceDetailConfig DEFAULT_REFERENCE_CONFIG = new ObjectImportRegistry.ReferenceDetailConfig();

    private final ObjectImportRegistry properties;


    public ObjectImportRegistry.ObjectConfig getObject(String objectName) {
        return properties.getObjects().getOrDefault(objectName, DEFAULT_OBJECT_IMPORT_CONFIG);
    }

    public ObjectImportRegistry.ReferenceDetailConfig getReferenceConfig(String objectName, String fieldName) {
        return getObject(objectName).getReferences().getOrDefault(fieldName, DEFAULT_REFERENCE_CONFIG);
    }

    public ObjectImportRegistry.ProcessingConfig getProcessingConfig(String objectName) {
        ObjectImportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getProcessing() == null ? properties.getProcessing() : objectConfig.getProcessing();
    }

    public ObjectImportRegistry.HeaderConfig getHeader(String objectName) {
        ObjectImportRegistry.ObjectConfig objectConfig = getObject(objectName);
        return objectConfig.getSource().getHeader() == null ? properties.getHeader() : objectConfig.getSource().getHeader();
    }

    public List<String> getLookupAttributeNames(String objectName, Attribute attribute) {

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = getReferenceConfig(objectName, attribute.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();
        if(Utils.CL.isEmpty(attributeLookups)) {
            return Utils.CL.newArrayList();
        }
        return attributeLookups
                .stream()
                .filter(lookupValuesConfig -> Utils.CL.isEmpty(lookupValuesConfig.getDefaultValues()))
                .map(ObjectImportRegistry.LookupValuesConfig::getAttributeName)
                .collect(Collectors.toList());
    }

    public List<String> getSourceFieldNames(String objectName, Attribute attribute) {

        ObjectImportRegistry.ReferenceDetailConfig referenceDetailConfig = getReferenceConfig(objectName, attribute.getAttributeRef().getFieldName());
        List<ObjectImportRegistry.LookupValuesConfig> attributeLookups = referenceDetailConfig.getLookups();

        if(Utils.CL.isEmpty(attributeLookups)) {
            return Utils.CL.newArrayList();
        }

        return attributeLookups
                .stream()
                .filter(lookupValuesConfig -> Utils.CL.isEmpty(lookupValuesConfig.getDefaultValues()))
                .map(lookupValuesConfig -> Utils.STR.defaultIfBlank(lookupValuesConfig.getSourceField(), attribute.getFieldName()))
                .collect(Collectors.toList());
    }

    /** Import of the object's rows as details of one header, or null when the object has no {@code parent} config. */
    public ObjectImportRegistry.ParentConfig getParent(String objectName) {
        return getObject(objectName).getParent();
    }

    /**
     * Identity of a row: within the header ({@code parent.field} then {@code parent.identity}) when the job imports
     * details of one header, the object's {@code identity} otherwise.
     */
    public List<String> getIdentityFields(String objectName, ImportContext context) {
        ObjectImportRegistry.ParentConfig parent = getParent(objectName);
        if (context == null || parent == null) {
            return getObject(objectName).getIdentity().getFields();
        }
        List<String> fields = new ArrayList<>(List.of(parent.getField()));
        parent.getIdentity().stream().filter(field -> !field.equals(parent.getField())).forEach(fields::add);
        return fields;
    }

    public String getSheet(String objectName) {
        return getObject(objectName).getSource().getSheet();
    }

    /**
     * Import order of all configured objects: every object comes after the objects in its {@code dependsOn}.
     * Objects without a dependency between them are ordered by name, so the result is stable.
     *
     * @throws ImportConfigurationException if a dependency is not configured or the dependencies form a cycle
     */
    public List<String> resolveOrder() {
        Map<String, ObjectImportRegistry.ObjectConfig> objects = properties.getObjects();
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String objectName : new TreeSet<>(objects.keySet())) {
            visit(objectName, objects, visited, new LinkedHashSet<>(), result);
        }
        return result;
    }

    /**
     * Objects imported from one workbook whose main sheet belongs to {@code rootObjectName}: the root first, then every
     * configured object that depends on it directly or transitively, in {@link #resolveOrder()} order.
     *
     * @throws ImportConfigurationException if a dependent object has no {@code source.sheet}; it would read the root's sheet
     */
    public List<String> getWorkbookObjects(String rootObjectName) {
        Map<String, ObjectImportRegistry.ObjectConfig> objects = properties.getObjects();
        Set<String> members = new HashSet<>(Set.of(rootObjectName));
        List<String> result = new ArrayList<>(List.of(rootObjectName));
        for (String objectName : resolveOrder()) {
            List<String> dependsOn = objects.get(objectName).getDependsOn();
            if (members.contains(objectName) || Utils.CL.isEmpty(dependsOn) || dependsOn.stream().noneMatch(members::contains)) {
                continue;
            }
            if (Utils.STR.isBlank(getSheet(objectName))) {
                throw new ImportConfigurationException(
                        "Import object [" + objectName + "] depends on [" + rootObjectName + "] but has no source.sheet");
            }
            members.add(objectName);
            result.add(objectName);
        }
        return result;
    }

    /** Fails fast on an invalid {@code dependsOn} or {@code parent} configuration. */
    public void validate() {
        resolveOrder();
        properties.getObjects().forEach((objectName, config) -> validateParent(objectName, config.getParent()));
    }

    private void validateParent(String objectName, ObjectImportRegistry.ParentConfig parent) {
        if (parent == null) {
            return;
        }
        if (Utils.STR.isBlank(parent.getObject()) || Utils.STR.isBlank(parent.getField())) {
            throw new ImportConfigurationException("Import object [" + objectName + "] parent needs object and field");
        }
        if (Utils.CL.isEmpty(parent.getModes()) || parent.getDefaultMode() == null || !parent.getModes().contains(parent.getDefaultMode())) {
            throw new ImportConfigurationException("Import object [" + objectName + "] parent default-mode must be one of its modes");
        }
        if (parent.getMaxRows() < 0) {
            throw new ImportConfigurationException("Import object [" + objectName + "] parent max-rows must not be negative");
        }
    }

    private void visit(
            String objectName,
            Map<String, ObjectImportRegistry.ObjectConfig> objects,
            Set<String> visited,
            LinkedHashSet<String> path,
            List<String> result
    ) {
        if (visited.contains(objectName)) {
            return;
        }
        if (!path.add(objectName)) {
            List<String> visiting = new ArrayList<>(path);
            List<String> cycle = new ArrayList<>(visiting.subList(visiting.indexOf(objectName), visiting.size()));
            cycle.add(objectName);
            throw new ImportConfigurationException("Circular import dependency: " + String.join(" -> ", cycle));
        }
        List<String> dependsOn = objects.get(objectName).getDependsOn();
        for (String dependency : Utils.CL.isEmpty(dependsOn) ? List.<String>of() : dependsOn) {
            if (!objects.containsKey(dependency)) {
                throw new ImportConfigurationException(
                        "Import object [" + objectName + "] depends on unknown object [" + dependency + "]");
            }
            visit(dependency, objects, visited, path, result);
        }
        path.remove(objectName);
        visited.add(objectName);
        result.add(objectName);
    }
}

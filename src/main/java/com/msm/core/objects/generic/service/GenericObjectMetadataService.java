package com.msm.core.objects.generic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.dynamicquery.mapping.JavaTypeMappingFactory;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.ObjectConstants;
import com.msm.core.validate.ObjectAttributeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.selectOne;

@Slf4j
@RequiredArgsConstructor
public class GenericObjectMetadataService {
    private final DSLContext dsl;

    public Optional<ObjectMetadata> getObjectMetadata(String objectName) {
        String objectKey = Utils.STR.lowCase(objectName);
        Optional<ObjectMetadata> objectAttribute = ObjectMetadataFactory.getObjectMetadata(objectKey);
        if(objectAttribute.isPresent()) {
            return objectAttribute;
        }
        Map<String, Object> result = dsl
                .select(field("meta_data"))
                .from("object_metadata")
                .where(field("object_type").eq(objectName))
                .fetchOneMap();

        if(Utils.CL.isEmpty(result)) {
            log.warn("Object metadata resource {} not found", objectKey);
            return Optional.empty();
        }
        Object objectMetadata = result.get("meta_data");
        try {
            ObjectMetadata objectMetadataObject = Utils.O.toObject(objectMetadata.toString(), ObjectMetadata.class);
            ObjectMetadataFactory.registerObjectMetadata(objectMetadataObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return ObjectMetadataFactory.getObjectMetadata(objectKey);
    }


    public void insert(ObjectMetadata meta, String serviceName) {
        try {
            dsl.insertInto(DSL.table("object_metadata"))
                    .columns(
                            field("name"),
                            field("description"),
                            field("service_name"),
                            field("object_type"),
                            field("meta_data")
                    )
                    .values(
                            meta.getName(),
                            "Standard object",
                            serviceName,
                            meta.getName(),
                            JSONB.valueOf(Utils.O.toJsonString(meta))
                    ).execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    public ObjectMetadata autoGenerateObjectMetaDataHandler(Map<String, Object> request) {
        String tableName = String.valueOf(request.get("tableName"));
        String objectName = Utils.STR.toCamelCaseUnderscore(tableName).toLowerCase();
        String schema = String.valueOf(request.get("schema"));
        String serviceName = String.valueOf(request.get("serviceName"));

        boolean exists = dsl.fetchExists(selectOne()
                        .from("object_metadata")
                        .where(field("object_type").eq(objectName)
                                .and(field("service_name").eq(serviceName))));
        if (exists) {
            throw new RuntimeException("Object metadata already exists");
        }

        List<String> excludeRefs = Utils.CL.emptyIfNull((List<String>) request.get("excludeRef"));
        List<String> freeTextColumns = Utils.CL.emptyIfNull((List<String>) request.get("freeTextColumns"));
        List<String> requiredColumns = Utils.CL.emptyIfNull((List<String>) request.get("requiredColumns"));

        List<Map<String, Object>> rows = fetchColumns(tableName, schema);

        List<Attribute> attributes = rows.stream()
                .map(map -> mapColumn(map, excludeRefs, freeTextColumns, requiredColumns))
                .collect(Collectors.toList());
        ObjectMetadata objectMetadata = ObjectMetadata.builder()
                .name(objectName)
                .tableName(tableName)
                .attributes(attributes)
                .build();

        insert(objectMetadata, serviceName);
        //add object metadata to cache
        getObjectMetadata(objectName);
        return objectMetadata;
    }

    private List<Map<String, Object>> fetchColumns(String tableName, String schema) {

        return dsl.select(
                    field("column_name", String.class),
                    field("data_type", String.class),
                    field("udt_name", String.class),
                    field("character_maximum_length", Integer.class),
                    field("is_nullable", String.class),
                    field("column_default", String.class)
                )
                .from("information_schema.columns")
                .where(field("table_name").eq(tableName))
                .and(field("table_schema").eq(schema))
                .fetchMaps();
    }

    private Attribute mapColumn(Map<String, Object> row, List<String> excludeRefs, List<String> freeTextColumns, List<String> requiredColumns) {

        String column = (String) row.get("column_name");
        String dataType = (String) row.get("data_type");
        String udtName = (String) row.get("udt_name");
        Integer maxLength = (Integer) row.get("character_maximum_length");
        String nullable = (String) row.get("is_nullable");
        String defaultVal = (String) row.get("column_default");
        String fieldName = Utils.STR.toCamelCaseUnderscore(column);

        boolean isRequired = requiredColumns.contains(fieldName);
        if (!isRequired) {
            isRequired = !"YES".equalsIgnoreCase(nullable);
        }

        if("gen_random_uuid()".equals(defaultVal)) {//ignore gen_random_uuid()
            isRequired = false;
            defaultVal = null;
        }

        boolean isFreeText = freeTextColumns.contains(fieldName);

        Attribute.AttributeBuilder attributeBuilder = Attribute.builder()
                .fieldName(fieldName)
                .columnName(column)
                .fieldType(JavaTypeMappingFactory.mapType(dataType, udtName))
                .defaultValue(defaultVal)
                .attributeRef(buildRef(column, excludeRefs));

        if(maxLength != null) {
            attributeBuilder.maxLength(Long.parseLong(maxLength.toString()));
        }

        if(isRequired) {
            attributeBuilder.isRequired(isRequired);
        }

        if(isFreeText) {
            attributeBuilder.isFreeText(true);
        }

        return attributeBuilder.build();
    }

    private String buildRef(String column, List<String> excludeRefs) {
        String fieldName = Utils.STR.toCamelCaseUnderscore(column);
        if(Utils.STR.lowCase(column).endsWith("_id") && !excludeRefs.contains(fieldName)) {
            return fieldName + "Reference";
        }
        return null;
    }

    public Optional<ObjectMetadata> getObjectAttribute0(String objectName) {
        String objectKey = Utils.STR.lowCase(objectName);
        Optional<ObjectMetadata> objectAttribute = ObjectAttributeFactory.get(objectKey);
        if(objectAttribute.isPresent()) {
            return objectAttribute;
        }
        try {
            ClassPathResource attributeResource = new ClassPathResource(Utils.STR.format(ObjectConstants.ATTRIBUTE_PATH_TEMPLATE, objectKey));
            if(!attributeResource.exists()) {
                log.warn("Attribute resource {} not found", objectKey);
                return Optional.empty();
            }
            ObjectMetadata objectMetadata = new ObjectMapper().readValue(attributeResource.getInputStream(), new TypeReference<>() {});
            ObjectMetadataFactory.registerObjectMetadata(objectMetadata);
            ObjectAttributeFactory.register(objectKey,  objectMetadata);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return ObjectAttributeFactory.get(objectKey);
    }
}

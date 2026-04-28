package com.msm.core.objects.generic.converter;

import com.msm.core.metadata.ObjectMetadata;

import java.util.Map;

public interface ObjectMappingStrategy {
    String DEFAULT_OBJECT_TYPE = "default";
    String supportObjectType();
    //Read and convert a database object to a user object.
    Map<String, Object> from(ObjectMetadata meta, Map<String, Object> databaseObject);
    //Convert and write a user object to a database object.
    Map<String, Object> to(ObjectMetadata meta, Map<String, Object> payload);
}

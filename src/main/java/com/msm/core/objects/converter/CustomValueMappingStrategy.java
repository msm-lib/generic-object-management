package com.msm.core.objects.converter;

import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.strategy.TypedStrategy;

import java.util.Map;

public interface CustomValueMappingStrategy extends TypedStrategy<String> {
    String DEFAULT_OBJECT_TYPE = "default";
    //Read and convert a database object to a user object.
    void from(ObjectMetadata meta, Map<String, Object> databaseObject);
    //Convert and write a user object to a database object.
    void to(ObjectMetadata meta, Map<String, Object> payload);
}

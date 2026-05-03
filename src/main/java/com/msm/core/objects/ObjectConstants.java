package com.msm.core.objects;

import java.util.Map;

public class ObjectConstants {
    private ObjectConstants() {}

    public static final String ATTRIBUTE_PATH_TEMPLATE = "metadata/{0}.json";
    public static final String CUSTOM_VALUE_FIELD_NAME = "customValues";
    public static final String UNWRAPPED_CUSTOM_VALUES = "UnwrappedCustomValues";
    public static final String IS_DELETED_FIELD = "isDeleted";
    public static final String SUMMARY_BLANKET_ORDER_PRODUCT_CHANGE = "summaryBlanketOrderProductChange";
    public static final String AUTO_GENERATE_PRODUCT_ALLOCATE = "autoGenerateBlanketOrderProductAllocate";

    public static final String REF_SUFFIX = "{0}Reference";

    public static final Map<String, String> PREFIX_OBJECT_CODE = Map.of(
            "v2blanketorder", "B"
    );
}

package com.msm.core.objects.connector.internal;

public class ApiConstants {
    // Base Path
    public static final String BASE_GENERIC_OBJECTS = "/api/v1/cn/portal/generic/objects";

    // Sub Paths
    public static final String PATH_QUERY = "/api/v1/cn/portal/generic/objects/query";
    public static final String PATH_METADATA = "/api/v1/cn/portal/generic/objects/metadata";
    public static final String PATH_CONVERSION = "/api/v1/cn/portal/generic/objects/conversion";

    public static final String PATH_BY_OBJECT = "/api/v1/cn/portal/generic/objects/{objectName}";
    public static final String PATH_BULK = "/api/v1/cn/portal/generic/objects/{objectName}/bulk";
    public static final String PATH_LOOKUP = "/api/v1/cn/portal/generic/objects/{objectName}/lookup";
    public static final String PATH_FILTER = "/api/v1/cn/portal/generic/objects/{0}/filter";

    public static final String PATH_BY_ID = "/api/v1/cn/portal/generic/objects/{0}/{1}";
    public static final String PATH_ACTION = "/api/v1/cn/portal/generic/objects/{objectName}/{id}/{actionName}";
    public static final String PATH_BULK_ACTION = "/api/v1/cn/portal/generic/objects/{objectName}/bulk/{actionName}";

}
package com.msm.core.objects.connector;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.objects.ObjectConstants;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.connector.internal.ApiConstants;
import com.msm.core.objects.dto.QueryTemplate;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.service.imports.factory.ObjectServiceFactory;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class GenericObjectInternalService {
    private final GenericObjectConfigProperties genericObjectConfigProperties;
    private final RequestClient internalRestClient;

    public List<Map<String, Object>> getAllObjectByIds(String objectName, List<UUID> ids, List<String> returnFields) {
        if (Utils.CL.isEmpty(ids)) {
            return Utils.CL.newArrayList();
        }
        ObjectFilterRequest objectFilterRequest = ObjectFilterRequest
                .builder()
                .objectInfo(ObjectFilterRequest.ObjectInfo.of(objectName))
                .returnFields(returnFields)
                .filters(FilterGroup
                        .builder()
                        .operator(LogicalOperator.AND)
                        .conditions(Utils.CL.newArrayList(FilterCondition.create(Constants.OBJECT_PK, FilterOperator.IN, ids)))
                        .build())
                .build();
        String filterUrl = Utils.STR.format(ApiConstants.PATH_FILTER, objectName);
        PageResponse<Map<String, Object>> mapPageResponse =  internalRestClient.post(getBaseUrl(objectName), filterUrl, objectFilterRequest, PageResponse.class);;
        return mapPageResponse.getContents();
    }

    public Map<String, Object> getObjectById(String objectName, Object id, List<String> returnFields) {
        if (id == null) {
            return Utils.CL.newHashMap();
        }
        String filterUrl = Utils.STR.format(ApiConstants.PATH_BY_ID, objectName, id);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("returnFields", returnFields);
        return internalRestClient.get(getBaseUrl(objectName), filterUrl, queryParams, Map.class);
    }

    public Map<String, Object> getObjectById(String objectName, Object id) {
        if (id == null) {
            return Utils.CL.newHashMap();
        }
        String patByIdUrl = Utils.STR.format(ApiConstants.PATH_BY_ID, objectName, id);
        return internalRestClient.get(getBaseUrl(objectName), patByIdUrl, Map.class);
    }

    public PageResponse<Map<String, Object>> filter(String objectName, ObjectFilterRequest objectFilterRequest) {
        String filterUrl = Utils.STR.format(ApiConstants.PATH_FILTER, objectName);
        return internalRestClient.post(getBaseUrl(objectName), filterUrl, objectFilterRequest, PageResponse.class);
    }

    public Map<String, Object> query(String objectName, QueryTemplate queryTemplate) {
        return internalRestClient.post(getBaseUrl(objectName), ApiConstants.PATH_QUERY, queryTemplate, Map.class);
    }

    public List<Map<String, Object>> getAllObjects(String objectName) {
        String queryAllObjectUrl = Utils.STR.format(ApiConstants.PATH_BY_OBJECT, objectName);
        return internalRestClient.get(getBaseUrl(objectName), queryAllObjectUrl, List.class);
    }


    String getBaseUrl(String objectName) {
        return ObjectConstants.ENV_MAP.get(genericObjectConfigProperties.getImportFile().getEnv()) + "/" + ObjectServiceFactory.getServiceName(objectName);
    }

}

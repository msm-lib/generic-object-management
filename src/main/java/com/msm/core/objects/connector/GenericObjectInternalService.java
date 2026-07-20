package com.msm.core.objects.connector;

import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.connector.internal.ApiNamedConstants;
import com.msm.core.objects.dto.QueryTemplate;
import com.msm.core.objects.integration.RequestClient;
import com.msm.core.objects.service.imports.factory.ObjectServiceFactory;
import com.msm.core.objects.utils.GenericObjectUtils;
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
        String filterUrl = Utils.STR.format(ApiNamedConstants.External.PATH_FILTER, objectName);
        PageResponse<Map<String, Object>> mapPageResponse =  internalRestClient.post(getBaseUrl(objectName), filterUrl, objectFilterRequest, PageResponse.class);;
        return mapPageResponse.getContents();
    }

    public Map<String, Object> getObjectById(String objectName, Object id, List<String> returnFields) {
        if (id == null) {
            return Utils.CL.newHashMap();
        }
        String filterUrl = Utils.STR.format(ApiNamedConstants.External.PATH_BY_ID, objectName, id);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("returnFields", returnFields);
        return internalRestClient.get(getBaseUrl(objectName), filterUrl, queryParams, Map.class);
    }

    public Map<String, Object> getObjectById(String objectName, Object id) {
        if (id == null) {
            return Utils.CL.newHashMap();
        }
        String patByIdUrl = Utils.STR.format(ApiNamedConstants.External.PATH_BY_ID, objectName, id);
        return internalRestClient.get(getBaseUrl(objectName), patByIdUrl, Map.class);
    }

    public PageResponse<Map<String, Object>> filter(String objectName, ObjectFilterRequest objectFilterRequest) {
        String filterUrl = Utils.STR.format(ApiNamedConstants.External.PATH_FILTER, objectName);
        return internalRestClient.post(getBaseUrl(objectName), filterUrl, objectFilterRequest, PageResponse.class);
    }

    public Map<String, Object> query(String objectName, QueryTemplate queryTemplate) {
        return internalRestClient.post(getBaseUrl(objectName), ApiNamedConstants.External.PATH_QUERY, queryTemplate, Map.class);
    }

    public List<Map<String, Object>> getAllObjects(String objectName) {
        String queryAllObjectUrl = Utils.STR.format(ApiNamedConstants.External.PATH_BY_OBJECT, objectName);
        return internalRestClient.get(getBaseUrl(objectName), queryAllObjectUrl, List.class);
    }


    String getBaseUrl(String objectName) {
        String baseUrl = genericObjectConfigProperties.getImportFile().getBasePathUrl();
        String serviceName = ObjectServiceFactory.getServiceName(objectName);
        if(isLocalhost(baseUrl)) {
            baseUrl = localhost(serviceName);
        }

        return GenericObjectUtils.normalizeUrl(baseUrl) + "/" + serviceName;
    }

    boolean isLocalhost(String baseUrl) {
        return Utils.STR.containsIgnoreCase(baseUrl, "localhost");
    }

    String localhost(String serviceName) {
        switch (serviceName) {
            case "user" -> {
                return "http://localhost:8086";
            }
            case "customer" -> {
                return "http://localhost:8087";
            }
            case "inventory" -> {
                return "http://localhost:8089";
            }
            case "master-data" -> {
                return "http://localhost:8090";
            }
            case "order" -> {
                return "http://localhost:8088";
            }
            default -> {
                return "http://localhost:8080";
            }
        }
    }

}

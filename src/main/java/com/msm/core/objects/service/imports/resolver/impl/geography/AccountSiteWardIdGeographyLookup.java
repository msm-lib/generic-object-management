package com.msm.core.objects.service.imports.resolver.impl.geography;

import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.repository.ObjectQueryRepository;

public class AccountSiteWardIdGeographyLookup extends AbstractGeographyLookup {
    private final String GEOGRAPHY_TYPE_VALUE = "11111111-0001-0001-0001-000000000008";

    public AccountSiteWardIdGeographyLookup(ObjectQueryRepository internalObjectQueryRepository, GenericObjectInternalService genericObjectInternalService) {
        super(internalObjectQueryRepository, genericObjectInternalService);
    }


    @Override
    public String sourceObject() {
        return "accountsite";
    }

    @Override
    public String targetObject() {
        return "geographylocation";
    }

    @Override
    public String sourceAttribute() {
        return "wardId";
    }

//
//    @Override
//    public List<String> attributes() {
//        return List.of(
//                "continentId",
//                "sectorId",
//                "countryId",
//                "regionId",
//                "areaId",
//                "provinceId",
//                "districtId",
//                "wardId"
//        );
//    }

    @Override
    String getGeographyTypeId() {
        return GEOGRAPHY_TYPE_VALUE;
    }

}

package com.msm.core.objects.dataexchange.imports.reference;

import com.msm.core.commons.Utils;
import com.msm.core.objects.dataexchange.imports.model.AttributeLookup;

import java.util.List;
import java.util.Set;

public final class RefConstants {
    private RefConstants() {}
    public static final String GEOGRAPHY_TYPE_ID = "geographyTypeId";
    public static final AttributeLookup ATTRIBUTE_CODE_LOOKUP = AttributeLookup.ofDefault("code");
    public static final List<AttributeLookup> ATTRIBUTE_CONTINENT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000001"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_SECTOR_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000002"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_COUNTRY_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000003"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_REGION_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000004"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_AREA_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000005"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_PROVINCE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000006"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_DISTRICT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000007"))
    );
    public static final List<AttributeLookup> ATTRIBUTE_WARD_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000008"))
    );



    public static final String ACCOUNT_ATTRIBUTE_TYPE_NAME = "type";
    public static final List<AttributeLookup> ATTRIBUTE_CHANNEL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CHANNEL"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_SUB_CHANNEL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_CHANNEL"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_CHANNEL_DETAIL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CHANNEL_DETAIL"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_SUB_DETAIL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_DETAIL"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_BIZ_TYPE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("BIZ_TYPE"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_SUB_TYPE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_TYPE"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_TRANSACTION_STATUS_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("TRANSACTION_STATUS"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_CLASS_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CLASS"))
    );

    public static final List<AttributeLookup> ATTRIBUTE_ACCOUNT_SEGMENT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("ACCOUNT_SEGMENT"))
    );

}

package com.msm.core.objects.imports.reference;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.objects.imports.ImportActionNamed;
import com.msm.core.objects.imports.model.AttributeLookup;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class AttributeReferenceService {
    private final AttributeCodeReferenceResolver attributeCodeReferenceResolver;
    private final TypeAndCodeReferenceResolver typeAndCodeReferenceResolver;



    //Default ref by code
    @Handler(action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> codeRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return attributeCodeReferenceResolver.resolve(attributeReferenceContext.importObjectName(), attributeReferenceContext.attribute(), attributeReferenceContext.data());
    }


    private static final String GEOGRAPHY_TYPE_ID = "geographyTypeId";
    private static final AttributeLookup ATTRIBUTE_CODE_LOOKUP = AttributeLookup.of("code", null);
    private static final List<AttributeLookup> ATTRIBUTE_CONTINENT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000001"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_SECTOR_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000002"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_COUNTRY_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000003"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_REGION_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000004"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_AREA_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000005"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_PROVINCE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000006"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_DISTRICT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000007"))
    );
    private static final List<AttributeLookup> ATTRIBUTE_WARD_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(GEOGRAPHY_TYPE_ID, Set.of("11111111-0001-0001-0001-000000000008"))
    );




    @Handler(resource = "accountsite.continentId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> continentIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_CONTINENT_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.sectorId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> sectorIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_SECTOR_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.countryId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> countryIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_COUNTRY_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.regionId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> regionIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_REGION_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.areaId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> areaIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_AREA_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.provinceId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> provinceIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_PROVINCE_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.districtId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> districtIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_DISTRICT_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.wardId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> wardIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_WARD_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }









    private static final String ACCOUNT_ATTRIBUTE_TYPE_NAME = "type";
    private static final List<AttributeLookup> ATTRIBUTE_CHANNEL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CHANNEL"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_SUB_CHANNEL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_CHANNEL"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_CHANNEL_DETAIL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CHANNEL_DETAIL"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_SUB_DETAIL_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_DETAIL"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_BIZ_TYPE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("BIZ_TYPE"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_SUB_TYPE_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("SUB_TYPE"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_TRANSACTION_STATUS_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("TRANSACTION_STATUS"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_CLASS_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("CLASS"))
    );

    private static final List<AttributeLookup> ATTRIBUTE_ACCOUNT_SEGMENT_LOOKUP = Utils.CL.newArrayList(
            ATTRIBUTE_CODE_LOOKUP,
            AttributeLookup.of(ACCOUNT_ATTRIBUTE_TYPE_NAME, Set.of("ACCOUNT_SEGMENT"))
    );




    @Handler(resource = "accountsite.channelId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> channelIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_CHANNEL_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.subChannelId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> subChannelIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_SUB_CHANNEL_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.channelDetailId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> channelDetailIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_CHANNEL_DETAIL_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.subDetailId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> subDetailIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_SUB_DETAIL_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.bizTypeId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> bizTypeIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_BIZ_TYPE_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.subTypeId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> subTypeIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_SUB_TYPE_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.transactionStatusId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> transactionStatusIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_TRANSACTION_STATUS_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.classId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> classIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_CLASS_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }

    @Handler(resource = "accountsite.accountSegmentId", action = ImportActionNamed.Csv.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> accountSegmentIdRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return typeAndCodeReferenceResolver.resolve(
                attributeReferenceContext.importObjectName(),
                ATTRIBUTE_ACCOUNT_SEGMENT_LOOKUP,
                attributeReferenceContext.attribute(),
                attributeReferenceContext.data()
        );
    }
}

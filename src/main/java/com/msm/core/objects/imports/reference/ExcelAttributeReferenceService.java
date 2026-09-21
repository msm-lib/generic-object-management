package com.msm.core.objects.imports.reference;

import com.msm.core.action.context.ActionContext;
import com.msm.core.objects.imports.model.AttributeReferenceContext;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Deprecated
@RequiredArgsConstructor
public class ExcelAttributeReferenceService {
    private final AttributeCodeReferenceResolver attributeCodeReferenceResolver;
    private final AttributeReferenceResolver attributeReferenceResolver;



    //Default ref by code
//    @Handler(action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
    public Map<String, Map<String, Map<String, Object>>> codeRef(ActionContext<AttributeReferenceContext> actionContext) {
        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
        return attributeReferenceResolver.resolve(attributeReferenceContext.importObjectName(), attributeReferenceContext.attribute(), attributeReferenceContext.data());
    }





//    @Handler(resource = "accountsite.continentId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> continentIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_CONTINENT_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.sectorId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> sectorIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_SECTOR_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.countryId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> countryIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_COUNTRY_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.regionId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> regionIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_REGION_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.areaId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> areaIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_AREA_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.provinceId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> provinceIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_PROVINCE_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.districtId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> districtIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_DISTRICT_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.wardId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> wardIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_WARD_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//
//
//
//
//
//
//    @Handler(resource = "accountsite.channelId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> channelIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_CHANNEL_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.subChannelId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> subChannelIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_SUB_CHANNEL_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.channelDetailId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> channelDetailIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_CHANNEL_DETAIL_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.subDetailId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> subDetailIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_SUB_DETAIL_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.bizTypeId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> bizTypeIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_BIZ_TYPE_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.subTypeId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> subTypeIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_SUB_TYPE_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.transactionStatusId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> transactionStatusIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_TRANSACTION_STATUS_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.classId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> classIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_CLASS_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
//
//    @Handler(resource = "accountsite.accountSegmentId", action = ObjectActionNamed.Excel.FIELD_REFERENCE_RESOLVE)
//    public Map<String, Map<String, Map<String, Object>>> accountSegmentIdRef(ActionContext<AttributeReferenceContext> actionContext) {
//        AttributeReferenceContext attributeReferenceContext = actionContext.getPayload();
//        return typeAndCodeReferenceResolver.resolve(
//                attributeReferenceContext.importObjectName(),
////                RefConstants.ATTRIBUTE_ACCOUNT_SEGMENT_LOOKUP,
//                attributeReferenceContext.attribute(),
//                attributeReferenceContext.data()
//        );
//    }
}

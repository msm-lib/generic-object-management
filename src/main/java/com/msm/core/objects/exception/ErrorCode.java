package com.msm.core.objects.exception;

public final class ErrorCode {

    public static final String USER_NOT_FOUND = "001";
    public static final String NO_DATA = "002";
    public static final String SAVE_FAILED = "003";
    public static final String DELETE_ERROR = "004";
    public static final String DUPLICATE_LABEL = "005";
    public static final String DUPLICATE_SYSTEM_KEY = "006";
    public static final String EXCEED_ATTRIBUTES_IN_GROUP = "007";
    public static final String DUPLICATE_GROUP_NAME = "008";
    public static final String MISSING_REQUIRED_FIELD_CODE = "009";
    public static final String MISSING_REQUIRED_HEADER_CODE = "010";
    public static final String INVALID_FORMAT_CODE = "011";
    public static final String ROLES_NOT_FOUND_CODE = "012";
    public static final String GROUP_HAS_ATTRIBUTES = "013";
    public static final String EXCEED_ATTRIBUTES_GROUP_NUMBER = "014";
    public static final String CANNOT_CHANGE_ATTRIBUTE_TYPE = "015";
    public static final String USER_NOT_FOUND_BY_ID = "016";
    public static final String INVALID_JWT_SIGNATURE_CODE = "017";
    public static final String GROUP_NOT_FOUND_CODE = "018";
    public static final String INVALID_ATTRIBUTES_TYPE_CODE = "019";
    public static final String DUPLICATE_ATTRIBUTES_LIST_VALUE_CODE = "020";
    public static final String EXCEED_ATTRIBUTES_LIST_VALUE_CODE = "021";
    public static final String INVALID_ATTRIBUTES_DATA_CODE = "022";
    public static final String ATTRIBUTE_LIST_VALUE_MIN_CODE = "023";
    public static final String ATTRIBUTE_PARENT_REQUIRE_CODE = "024";
    public static final String ATTRIBUTE_MODIFY_BY_ANOTHER_USER = "025";
    public static final String ATTRIBUTE_NOT_FOUND_CODE = "026";
    public static final String ATTRIBUTE_VALUE_IS_EXCEEDING_CHARACTERS_CODE = "027";
    public static final String ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_DATE_CODE = "028";
    public static final String ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_NUMBER_CODE = "029";
    public static final String DUPLICATE_OBJECT_NAME = "030";
    public static final String NOT_FOUND = "031";
    public static final String INVALID_INPUT_FIELD = "032";
    public static final String INVALID_ATTR_DATA_TYPE_CODE = "033";
    public static final String DUPLICATE_RECORD = "034";
    public static final String OBJECT_META_DATA_NOT_FOUND = "035";
    public static final String OBJECT_NOT_FOUND = "036";
    public static final String INVALID_UPDATE_PAYLOAD = "037";


    public static final String UNAUTHORIZED_CODE = "097";
    public static final String GENERIC_TECHNICAL_ERROR_CODE = "098";
    public static final String GENERIC_BUSINESS_ERROR_CODE = "099";

    private ErrorCode() {
    }
}

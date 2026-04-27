package com.msm.core.objects.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceErrorEnum {
    USER_NOT_FOUND(ErrorCode.USER_NOT_FOUND, "User not found by email {0}"),
    NO_DATA(ErrorCode.NO_DATA, "No content data."),
    SAVE_FAILED(ErrorCode.SAVE_FAILED, "Save failed."),
    DELETE_ERROR(ErrorCode.DELETE_ERROR, "Delete error."),
    DUPLICATE_LABEL(ErrorCode.DUPLICATE_LABEL, "Duplicate label."),
    DUPLICATE_SYSTEM_KEY(ErrorCode.DUPLICATE_SYSTEM_KEY, "Duplicate System Key."),
    EXCEED_ATTRIBUTES_IN_GROUP(ErrorCode.EXCEED_ATTRIBUTES_IN_GROUP, "Exceed attributes in group."),
    DUPLICATE_GROUP_NAME(ErrorCode.DUPLICATE_GROUP_NAME, "Duplicate group name."),
    DUPLICATE_OBJECT_NAME(ErrorCode.DUPLICATE_OBJECT_NAME, "Duplicate object name."),
    MISSING_REQUIRED_HEADER(ErrorCode.MISSING_REQUIRED_HEADER_CODE, "Missing required header: {0}"),
    MISSING_REQUIRED_FILED(ErrorCode.MISSING_REQUIRED_FIELD_CODE, "Missing required field: {0}"),
    ROLES_NOT_FOUND(ErrorCode.ROLES_NOT_FOUND_CODE, "You don't have role to access this site."),
    INVALID_FORMAT(ErrorCode.INVALID_FORMAT_CODE, "Invalid format, field: {0}"),
    UNAUTHORIZED(ErrorCode.UNAUTHORIZED_CODE, "Unauthorized"),
    GROUP_HAS_ATTRIBUTES(ErrorCode.GROUP_HAS_ATTRIBUTES, "Group has attributes can not delete"),
    GROUP_NOT_FOUND(ErrorCode.GROUP_NOT_FOUND_CODE, "Group not found"),
    EXCEED_ATTRIBUTES_GROUP_NUMBER(ErrorCode.EXCEED_ATTRIBUTES_GROUP_NUMBER, "over limit attributes group number"),
    CANNOT_CHANGE_ATTRIBUTE_TYPE(ErrorCode.CANNOT_CHANGE_ATTRIBUTE_TYPE, "the attribute type doesn't allow to change"),
    USER_NOT_FOUND_BY_ID(ErrorCode.USER_NOT_FOUND_BY_ID, "User not found by id {0}"),
    INVALID_ATTRIBUTES_TYPE(ErrorCode.INVALID_ATTRIBUTES_TYPE_CODE, "Invalid AttributesType key: {0}"),
    DUPLICATE_ATTRIBUTES_LIST_VALUE(ErrorCode.DUPLICATE_ATTRIBUTES_LIST_VALUE_CODE, "Duplicate attribute value of attribute custom list"),
    EXCEED_ATTRIBUTES_LIST_VALUE(ErrorCode.EXCEED_ATTRIBUTES_LIST_VALUE_CODE, "Exceed value of attribute custom list"),
    INVALID_ATTRIBUTES_DATA(ErrorCode.INVALID_ATTRIBUTES_DATA_CODE, "Invalid attribute data"),
    ATTRIBUTE_LIST_VALUE_MIN(ErrorCode.ATTRIBUTE_LIST_VALUE_MIN_CODE, "Attribute list is not null or empty"),
    ATTRIBUTE_PARENT_REQUIRE(ErrorCode.ATTRIBUTE_PARENT_REQUIRE_CODE, "Attribute parent is require"),
    ATTRIBUTE_MODIFY_BY_ANOTHER_USER(ErrorCode.ATTRIBUTE_MODIFY_BY_ANOTHER_USER, "Attribute has been modified by another user. Please reload and try again."),
    ATTRIBUTE_NOT_FOUND(ErrorCode.ATTRIBUTE_NOT_FOUND_CODE, "Attribute not found with id {0}"),
    ATTRIBUTE_VALUE_IS_EXCEEDING_CHARACTERS(ErrorCode.ATTRIBUTE_VALUE_IS_EXCEEDING_CHARACTERS_CODE, "Attribute value exceed allowed number of character."),
    ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_DATE(ErrorCode.ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_DATE_CODE, "Attribute value is not in correct date format."),
    ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_NUMBER(ErrorCode.ATTRIBUTE_VALUE_IS_CORRECT_FORMAT_NUMBER_CODE, "Attribute value is not in correct number format."),

    NOT_FOUND(ErrorCode.NOT_FOUND, "{0} not found with {}"),
    INVALID_ATTR_DATA_TYPE(ErrorCode.INVALID_ATTR_DATA_TYPE_CODE, "Invalid data type"),
    DUPLICATE_RECORD(ErrorCode.DUPLICATE_RECORD, "This {0} is already in use"),
    OBJECT_META_DATA_NOT_FOUND(ErrorCode.OBJECT_META_DATA_NOT_FOUND, "Object metadata not found"),

    INVALID_JWT_SIGNATURE(ErrorCode.INVALID_JWT_SIGNATURE_CODE, "Invalid jwt token"),
    GENERIC_TECHNICAL_ERROR(ErrorCode.GENERIC_TECHNICAL_ERROR_CODE, "Generic Technical Error"),
    GENERIC_BUSINESS_ERROR(ErrorCode.GENERIC_BUSINESS_ERROR_CODE, "Generic Business Error");

    private final String code;
    private final String message;
}

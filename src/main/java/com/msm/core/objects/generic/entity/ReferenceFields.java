package com.msm.core.objects.generic.entity;

import com.msm.core.commons.Utils;
import com.msm.core.objects.generic.ObjectConstants;

import java.util.List;

public interface ReferenceFields {
    List<String> referenceFields();
    default String referenceField(String field) {
        return Utils.STR.format(ObjectConstants.REF_SUFFIX, field);
    }
}

package com.msm.core.objects.audit;

import com.msm.core.commons.Constants;
import com.msm.core.dynamicquery.context.RequestContext;
import com.msm.core.dynamicquery.context.RequestContextHolder;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;

import java.time.Instant;
import java.util.Map;

public class DefaultAuditStrategy implements AuditStrategy {

    @Override
    public String supportObjectType() {
        return DEFAULT_OBJECT_TYPE; // fallback
    }

    @Override
    public void apply(AuditAction action, ObjectMetadata meta, Map<String, Object> payload) {
        RequestContext ctx = RequestContextHolder.getRequestContext();
        Instant now = Instant.now();
        switch (action) {
            case CREATE -> {
                put(meta, payload, Constants.CREATED_AT, now);
                put(meta, payload, Constants.CREATED_BY, ctx.getUsername());
                put(meta, payload, Constants.CREATED_BY_ID, ctx.getUserId());
                put(meta, payload, Constants.IS_DELETED, Boolean.FALSE);
            }
            case UPDATE -> {
                put(meta, payload, Constants.UPDATED_AT, now);
                put(meta, payload, Constants.UPDATED_BY, ctx.getUsername());
                put(meta, payload, Constants.UPDATED_BY_ID, ctx.getUserId());
            }
            case DELETE -> {
                put(meta, payload, Constants.DELETED_AT, now);
                put(meta, payload, Constants.DELETED_BY, ctx.getUsername());
                put(meta, payload, Constants.DELETED_BY_ID, ctx.getUserId());
                put(meta, payload, Constants.IS_DELETED, Boolean.TRUE);
            }
        }
    }

    private void put(ObjectMetadata meta, Map<String, Object> payload, String attrName, Object value) {

        Attribute attr = meta.getAttributeByName(attrName);
        if (attr != null) {
            payload.put(attr.getFieldName(), value);
        }
    }
}
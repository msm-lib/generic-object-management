package com.msm.core.objects.audit;

import com.msm.core.commons.Constants;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.security.RequestContextHolder;
import com.msm.core.security.context.RequestContext;
import com.msm.core.security.model.Team;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class DefaultAuditStrategy implements AuditStrategy {

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
                putTeam(meta, payload, ctx.getTeam());
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

    private void putTeam(ObjectMetadata meta, Map<String, Object> payload, Team team) {
        Attribute attrTeam = meta.getAttributeByName(Constants.TEAM_ID);
        if (attrTeam != null) {
            payload.putIfAbsent(Constants.TEAM_ID, getTeamId(team));
            payload.putIfAbsent(Constants.TEAM_ID_REF, team);
        }
    }

    @Override
    public String support() {
        return DEFAULT_OBJECT_TYPE; // fallback
    }

    private Object getTeamId(Team team) {
        return Objects.nonNull(team) ? team.getId() : null;
    }

}
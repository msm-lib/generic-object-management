package com.msm.core.objects.audit.changelog;

import com.msm.core.commons.Constants;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.logging.LogTraceUtils;
import com.msm.core.security.RequestContextHolder;
import com.msm.core.security.context.RequestContext;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the entries of {@link ChangeLogEntry} and hands them to the {@link ChangeLogCollector}. Errors are not caught:
 * a write whose change log cannot be recorded fails, so no change is committed without its log.
 */
@RequiredArgsConstructor
public class DefaultChangeLogRecorder implements ChangeLogRecorder {

    static final String MASK = "***";
    static final String TRUNCATED = "...(truncated)";
    private static final UUID NO_USER = new UUID(0L, 0L);

    private final ChangeLogProperties properties;
    private final ChangeLogCollector collector;
    private final ChangeLogDiff diff;

    @Override
    public boolean isEnabled(ObjectMetadata meta) {
        return meta != null && properties.objectConfig(meta.getName()) != null;
    }

    @Override
    public void recordCreate(ObjectMetadata meta, List<Map<String, Object>> afters) {
        ChangeLogProperties.ObjectConfig config = properties.objectConfig(meta.getName());
        if (config == null || afters == null || afters.isEmpty()) {
            return;
        }
        Context context = context();
        List<ChangeLogEntry> entries = new ArrayList<>();
        for (Map<String, Object> after : afters) {
            if (after != null) {
                entries.add(entry(meta, config, context, ChangeLogEntry.ACTION_CREATE, after,
                        diff.created(meta, after, ignore(config)), null));
            }
        }
        collector.add(entries);
    }

    @Override
    public void recordUpdate(ObjectMetadata meta, List<Map<String, Object>> befores, List<Map<String, Object>> afters) {
        ChangeLogProperties.ObjectConfig config = properties.objectConfig(meta.getName());
        if (config == null || afters == null || afters.isEmpty()) {
            return;
        }
        Context context = context();
        Map<String, Map<String, Object>> beforeById = byId(meta, befores);
        Set<String> ignore = ignore(config);
        List<ChangeLogEntry> entries = new ArrayList<>();
        for (Map<String, Object> after : afters) {
            if (after == null) {
                continue;
            }
            Map<String, Object> before = beforeById.get(id(meta, after));
            if (before == null) {
                entries.add(entry(meta, config, context, ChangeLogEntry.ACTION_CREATE, after, diff.created(meta, after, ignore), null));
                continue;
            }
            List<ChangedField> changes = diff.diff(meta, before, after, ignore);
            if (changes.isEmpty()) {
                continue;
            }
            boolean softDeleted = !Boolean.TRUE.equals(before.get(Constants.IS_DELETED)) && Boolean.TRUE.equals(after.get(Constants.IS_DELETED));
            entries.add(softDeleted
                    ? entry(meta, config, context, ChangeLogEntry.ACTION_DELETE, after, changes, Map.of("deleteType", "SOFT"))
                    : entry(meta, config, context, ChangeLogEntry.ACTION_UPDATE, after, changes, null));
        }
        collector.add(entries);
    }

    @Override
    public void recordDelete(ObjectMetadata meta, List<Map<String, Object>> befores, boolean soft) {
        ChangeLogProperties.ObjectConfig config = properties.objectConfig(meta.getName());
        if (config == null || befores == null || befores.isEmpty()) {
            return;
        }
        Context context = context();
        Map<String, Object> metadata = Map.of("deleteType", soft ? "SOFT" : "HARD");
        List<ChangeLogEntry> entries = new ArrayList<>();
        for (Map<String, Object> before : befores) {
            if (before != null) {
                entries.add(entry(meta, config, context, ChangeLogEntry.ACTION_DELETE, before,
                        diff.deleted(meta, before, ignore(config)), metadata));
            }
        }
        collector.add(entries);
    }

    private ChangeLogEntry entry(ObjectMetadata meta, ChangeLogProperties.ObjectConfig config, Context context, String action,
                                 Map<String, Object> row, List<ChangedField> changes, Map<String, Object> metadata) {
        return new ChangeLogEntry(
                UUID.randomUUID(),
                Instant.now(),
                context.tenantCode(),
                properties.getSourceService(),
                context.txId(),
                action,
                meta.getName(),
                id(meta, row),
                config.getParentField() == null ? null : text(row.get(config.getParentField())),
                config.getCodeField() == null ? null : text(row.get(config.getCodeField())),
                context.actor(),
                sanitize(config, changes),
                metadata);
    }

    private List<ChangedField> sanitize(ChangeLogProperties.ObjectConfig config, List<ChangedField> changes) {
        Set<String> masked = new HashSet<>(properties.getMaskFields());
        masked.addAll(config.getMaskFields());
        List<ChangedField> sanitized = new ArrayList<>(changes.size());
        for (ChangedField change : changes) {
            if (masked.contains(change.name())) {
                sanitized.add(new ChangedField(change.name(), change.before() == null ? null : MASK,
                        change.after() == null ? null : MASK, change.dataType()));
            } else {
                sanitized.add(new ChangedField(change.name(), truncate(change.before()), truncate(change.after()), change.dataType()));
            }
        }
        return sanitized;
    }

    private Object truncate(Object value) {
        if (value instanceof String text && text.length() > properties.getMaxValueLength()) {
            return text.substring(0, properties.getMaxValueLength()) + TRUNCATED;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, item) -> out.put(String.valueOf(key), truncate(item)));
            return out;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::truncate).toList();
        }
        return value;
    }

    private Set<String> ignore(ChangeLogProperties.ObjectConfig config) {
        Set<String> ignore = new HashSet<>(properties.getIgnoreFields());
        ignore.addAll(config.getIgnoreFields());
        return ignore;
    }

    private Map<String, Map<String, Object>> byId(ObjectMetadata meta, List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byId = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row != null) {
                    byId.put(id(meta, row), row);
                }
            }
        }
        return byId;
    }

    private String id(ObjectMetadata meta, Map<String, Object> row) {
        Attribute idAttribute = meta.getIdAttribute();
        String idField = idAttribute != null ? idAttribute.getFieldName() : Constants.OBJECT_PK;
        return text(row.get(idField));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Context context() {
        RequestContext requestContext = requestContext();
        String traceId = LogTraceUtils.getTraceIdLog();
        String txId = traceId == null || traceId.isBlank() ? collector.txId() : traceId;
        String tenantCode = requestContext == null ? null : requestContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank() || "default".equals(tenantCode)) {
            tenantCode = properties.getDefaultTenant();
        }
        UUID userId = requestContext == null ? null : requestContext.getUserId();
        String username = requestContext == null ? null : requestContext.getUsername();
        ChangeLogActor actor = userId == null || NO_USER.equals(userId)
                ? new ChangeLogActor(null, ChangeLogActor.TYPE_SYSTEM, username == null ? "SYSTEM" : username)
                : new ChangeLogActor(userId.toString(), ChangeLogActor.TYPE_USER, username);
        return new Context(tenantCode, txId, actor);
    }

    /** Outside a request (job, async import) the service's provider may have no context to give. */
    private RequestContext requestContext() {
        try {
            return RequestContextHolder.getRequestContext();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private record Context(String tenantCode, String txId, ChangeLogActor actor) {
    }
}

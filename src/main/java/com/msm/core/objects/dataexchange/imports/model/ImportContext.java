package com.msm.core.objects.dataexchange.imports.model;

import com.msm.core.commons.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The header an import job writes details into (action {@code import_details}); kept in {@code fileInfo.context} of the
 * job. {@code parentReference} is the snapshot of the header stored on every detail row (the {@code <field>Reference} of
 * the detail's parent field).
 */
public record ImportContext(
        String parentObject,
        UUID parentId,
        String parentCode,
        Long parentVersion,
        ImportMode mode,
        Map<String, Object> parentReference
) {
    public static final String PARENT_OBJECT = "parentObject";
    public static final String PARENT_ID = "parentId";
    public static final String PARENT_CODE = "parentCode";
    public static final String PARENT_VERSION = "parentVersion";
    public static final String MODE = "mode";
    public static final String PARENT_REFERENCE = "parentReference";

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(PARENT_OBJECT, parentObject);
        map.put(PARENT_ID, parentId);
        map.put(PARENT_CODE, parentCode);
        map.put(PARENT_VERSION, parentVersion);
        map.put(MODE, mode == null ? null : mode.name());
        map.put(PARENT_REFERENCE, parentReference);
        return map;
    }

    /** The context stored in a job, or null when the job has none. */
    @SuppressWarnings("unchecked")
    public static ImportContext from(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.get(PARENT_ID) == null) {
            return null;
        }
        Object version = map.get(PARENT_VERSION);
        Object mode = map.get(MODE);
        Object reference = map.get(PARENT_REFERENCE);
        return new ImportContext(
                (String) map.get(PARENT_OBJECT),
                UUID.fromString(map.get(PARENT_ID).toString()),
                (String) map.get(PARENT_CODE),
                version instanceof Number number ? number.longValue() : null,
                mode == null ? null : ImportMode.valueOf(mode.toString()),
                reference instanceof Map<?, ?> referenceMap ? new HashMap<>((Map<String, Object>) referenceMap) : Utils.CL.newHashMap());
    }
}

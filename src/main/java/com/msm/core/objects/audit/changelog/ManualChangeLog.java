package com.msm.core.objects.audit.changelog;

import com.msm.core.commons.Constants;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.service.GenericObjectMetadataService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Change log of a write the service makes outside {@code ObjectQueryRepository} (JPA save, jOOQ / SQL written by hand).
 * The caller gives the rows in the field-name form of the object, and the entries go through the same recorder as a
 * repository write: same diff, mask, actor and tx id, written to the outbox in the running transaction (so the row and
 * its log commit or roll back together), then relayed to the notification service.
 *
 * <p>Call it after the write, inside the write's transaction. A row only needs the fields the write touches plus the
 * id, and the code / parent fields of the object's options; a field absent from both before and after is not a change.
 * The before must be a copy taken before the write, not the same map changed afterwards. Objects without a change log
 * record nothing, so the calls can stay in place whatever the configuration.
 */
@RequiredArgsConstructor
public class ManualChangeLog {

    private final ChangeLogRecorder changeLogRecorder;
    private final GenericObjectMetadataService genericObjectMetadataService;

    public void create(String objectName, Map<String, Object> after) {
        create(objectName, List.of(after));
    }

    public void create(String objectName, List<Map<String, Object>> afters) {
        record(objectName, meta -> {
            requireIds(meta, afters);
            changeLogRecorder.recordCreate(meta, afters);
        });
    }

    /** Rows are matched by id; fields with the same value before and after are left out, and no change logs nothing. */
    public void update(String objectName, Map<String, Object> before, Map<String, Object> after) {
        update(objectName, List.of(before), List.of(after));
    }

    public void update(String objectName, List<Map<String, Object>> befores, List<Map<String, Object>> afters) {
        record(objectName, meta -> {
            requireIds(meta, befores);
            requireIds(meta, afters);
            changeLogRecorder.recordUpdate(meta, befores, afters);
        });
    }

    /** {@code soft}: the row was flagged {@code isDeleted}, not removed. */
    public void delete(String objectName, Map<String, Object> before, boolean soft) {
        delete(objectName, List.of(before), soft);
    }

    public void delete(String objectName, List<Map<String, Object>> befores, boolean soft) {
        record(objectName, meta -> {
            requireIds(meta, befores);
            changeLogRecorder.recordDelete(meta, befores, soft);
        });
    }

    private void record(String objectName, Consumer<ObjectMetadata> recording) {
        ObjectMetadata meta = genericObjectMetadataService.getObjectMetadata(objectName)
                .orElseThrow(() -> ObjectErrors.notFound(objectName));
        if (changeLogRecorder.isEnabled(meta)) {
            recording.accept(meta);
        }
    }

    /** Without its id an entry has no entity id, and befores and afters cannot be matched. */
    private void requireIds(ObjectMetadata meta, List<Map<String, Object>> rows) {
        Attribute idAttribute = meta.getIdAttribute();
        String idField = idAttribute != null ? idAttribute.getFieldName() : Constants.OBJECT_PK;
        for (Map<String, Object> row : rows) {
            if (row == null || row.get(idField) == null) {
                throw new IllegalArgumentException("Change log of " + meta.getName() + ": every row needs its " + idField);
            }
        }
    }
}

package com.msm.core.objects.audit.changelog;

import com.msm.core.metadata.ObjectMetadata;

import java.util.List;
import java.util.Map;

/**
 * Records the changes made by {@code ObjectQueryRepository}. Rows are in the field-name form the repository hands out;
 * befores and afters are matched by id.
 */
public interface ChangeLogRecorder {

    /** Records nothing: for a repository built without a change log. */
    ChangeLogRecorder DISABLED = new ChangeLogRecorder() {
        @Override
        public boolean isEnabled(ObjectMetadata meta) {
            return false;
        }

        @Override
        public void recordCreate(ObjectMetadata meta, List<Map<String, Object>> afters) {
            // disabled
        }

        @Override
        public void recordUpdate(ObjectMetadata meta, List<Map<String, Object>> befores, List<Map<String, Object>> afters) {
            // disabled
        }

        @Override
        public void recordDelete(ObjectMetadata meta, List<Map<String, Object>> befores, boolean soft) {
            // disabled
        }
    };

    /** Whether the object has a change log; when not, the repository does not read the rows before writing. */
    boolean isEnabled(ObjectMetadata meta);

    void recordCreate(ObjectMetadata meta, List<Map<String, Object>> afters);

    /**
     * Rows read before the write and the same rows after it. An after without a before is a CREATE (upsert that
     * inserted); a row whose {@code isDeleted} goes to true is a soft DELETE; rows without change are skipped.
     */
    void recordUpdate(ObjectMetadata meta, List<Map<String, Object>> befores, List<Map<String, Object>> afters);

    void recordDelete(ObjectMetadata meta, List<Map<String, Object>> befores, boolean soft);
}

package com.msm.core.objects.audit.changelog;

import com.msm.core.commons.Constants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Change log of generic objects: every row written through {@code ObjectQueryRepository} for an object listed in
 * {@link #objects} gets an entry (before / after per field) in the outbox table {@code audit_log_outbox}, in the same
 * transaction as the row; {@link ChangeLogRelay} then sends the entries to the notification service.
 *
 * <pre>
 * objects:
 *   change-log:
 *     enabled: true
 *     source-service: ORDER_SERVICE
 *     objects: [v2order, v2orderitem]
 *     object-options:
 *       v2orderitem:
 *         parent-field: orderId
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "objects.change-log")
public class ChangeLogProperties {

    /** Master switch: off, nothing is recorded and the relay does not run. */
    private boolean enabled = false;

    /** {@code sourceService} of the notification audit log: USER_SERVICE, ORDER_SERVICE, CUSTOMER_SERVICE… */
    private String sourceService;

    /** Tenant of the entries written without a tenant in the request context (jobs); sent as header {@code x-tenant}. */
    private String defaultTenant;

    /**
     * Objects with a change log, by object name as in the generic object route ({@code v2order}). A list rather than map
     * keys: an entry written {@code v2order: {}} in YAML binds to nothing and would be dropped without a word.
     */
    private Set<String> objects = new HashSet<>();

    /** Options of the objects of {@link #objects} that need some, keyed by object name. */
    private Map<String, ObjectConfig> objectOptions = new HashMap<>();

    /** Fields never compared: the audit columns and the version say who / when, not what changed. */
    private Set<String> ignoreFields = new HashSet<>(Set.of(
            Constants.VERSION,
            Constants.CREATED_AT, Constants.CREATED_BY, Constants.CREATED_BY_ID,
            Constants.UPDATED_AT, Constants.UPDATED_BY, Constants.UPDATED_BY_ID,
            Constants.DELETED_AT, Constants.DELETED_BY, Constants.DELETED_BY_ID,
            Constants.CUSTOM_VALUE_NAME));

    /** Fields whose values are replaced by {@code ***} in every object (password, otp…). */
    private Set<String> maskFields = new HashSet<>();

    /** Longer text values are cut to this length. */
    private int maxValueLength = 2000;

    private Relay relay = new Relay();

    @Data
    public static class ObjectConfig {
        /** Field holding the business code of the row ({@code entityCode}). */
        private String codeField = "code";
        /** Field holding the id of the parent row ({@code entityParentId}), e.g. {@code orderId} for an order item. */
        private String parentField;
        private Set<String> ignoreFields = new HashSet<>();
        private Set<String> maskFields = new HashSet<>();
    }

    @Data
    public static class Relay {
        private boolean enabled = true;
        /**
         * Base URL of the notification service without the service path; blank: {@code objects.api-config.service-urls.notification},
         * then {@code objects.api-config.base-url}. "/notification" is appended.
         */
        private String url;
        private Duration interval = Duration.ofSeconds(30);
        private int batchSize = 200;
        /** An entry still failing after this many sends stays in status FAILED. */
        private int maxAttempts = 10;
        /** An entry claimed by an instance that died is sent again after this time. */
        private Duration lockTimeout = Duration.ofMinutes(5);
        /** Sent entries are deleted from the outbox after this time. */
        private Duration retention = Duration.ofDays(7);
    }

    /** Options of the object when it has a change log, else {@code null}. */
    public ObjectConfig objectConfig(String objectName) {
        if (!enabled || objectName == null) {
            return null;
        }
        String name = objectName.toLowerCase();
        if (objects.stream().noneMatch(object -> object.equalsIgnoreCase(name))) {
            return null;
        }
        return objectOptions.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(ObjectConfig::new);
    }
}

package com.msm.core.objects.audit.changelog;

/**
 * Who made the change, as {@code AuditActorDto} of the notification service.
 *
 * @param id user id, {@code null} for the system
 * @param type USER, or SYSTEM when no user is in the request context (jobs, internal calls)
 * @param displayName username
 */
public record ChangeLogActor(String id, String type, String displayName) {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_SYSTEM = "SYSTEM";
}

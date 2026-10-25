package com.msm.core.objects.audit.changelog;

/**
 * One field of a change, as {@code ChangedFieldDto} of the notification service.
 *
 * @param name field name
 * @param before value before the change, {@code null} for a new row
 * @param after value after the change, {@code null} for a deleted row
 * @param dataType text, number, boolean, datetime or date; {@code null} for objects and lists
 */
public record ChangedField(String name, Object before, Object after, String dataType) {
}

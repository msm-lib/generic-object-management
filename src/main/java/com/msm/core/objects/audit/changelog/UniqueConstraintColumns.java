package com.msm.core.objects.audit.changelog;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Columns of a unique constraint, in key order, read from the catalog: an upsert on a constraint name gives no columns,
 * and the change log needs them to read the rows the upsert may update. Found columns are cached for the life of the
 * process; an empty answer (wrong name, constraint not created yet) is not, so the next call reads the catalog again.
 */
@RequiredArgsConstructor
public class UniqueConstraintColumns {

    private static final String SQL = """
            select a.attname
            from pg_constraint c
            join pg_class t on t.oid = c.conrelid
            join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
            where c.conname = ? and t.relname = ? and pg_table_is_visible(t.oid)
            order by array_position(c.conkey, a.attnum)
            """;

    private final DSLContext dsl;
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    /** Column names in key order; empty when the table has no such constraint. */
    public List<String> columns(String tableName, String constraintName) {
        String key = tableName + "." + constraintName;
        List<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<String> columns = List.copyOf(dsl.resultQuery(SQL, constraintName, tableName).fetch(0, String.class));
        if (!columns.isEmpty()) {
            cache.put(key, columns);
        }
        return columns;
    }
}

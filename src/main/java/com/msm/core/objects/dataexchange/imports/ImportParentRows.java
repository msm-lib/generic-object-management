package com.msm.core.objects.dataexchange.imports;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Fills the header of an import into one header (action import_details) on each detail row: the parent field gets the
 * header id and its reference the header snapshot, so the file needs no header column and no row can point elsewhere.
 */
public class ImportParentRows {
    public static final String PARENT_MISMATCH = "PARENT_MISMATCH";

    /**
     * Fills the row; returns the error message when the file gives another header in the parent field (its id or code),
     * null otherwise.
     */
    public String fill(Map<String, Object> rowData, ImportContext context, ObjectImportRegistry.ParentConfig parent,
                       ObjectMetadata detailMetadata) {
        String field = parent.getField();
        Object given = rowData.get(field);
        if (given != null && Utils.STR.isNotBlank(given.toString())) {
            String value = given.toString().trim();
            boolean sameHeader = value.equalsIgnoreCase(context.parentId().toString()) || value.equals(context.parentCode());
            if (!sameHeader) {
                return field + ": '" + value + "' is not the " + context.parentObject() + " being imported into ("
                        + Utils.STR.defaultIfBlank(context.parentCode(), context.parentId().toString()) + ")";
            }
        }
        rowData.put(field, context.parentId());
        Attribute attribute = detailMetadata.getAttributeByName(field);
        if (attribute != null && attribute.hasRef()) {
            rowData.put(attribute.getAttributeRef().getFieldName(), new HashMap<>(Utils.CL.emptyIfNull(context.parentReference())));
        }
        return null;
    }
}

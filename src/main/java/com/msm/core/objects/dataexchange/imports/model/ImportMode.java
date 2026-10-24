package com.msm.core.objects.dataexchange.imports.model;

/** How an import into one header treats a row whose identity already exists among the header's details. */
public enum ImportMode {
    /** Insert only: a row matching an existing detail of the header is an error. */
    APPEND,
    /** Insert new rows, update the existing details of the header they match. */
    UPSERT
}

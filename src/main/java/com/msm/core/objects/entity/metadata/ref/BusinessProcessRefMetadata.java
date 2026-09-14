package com.msm.core.objects.entity.metadata.ref;

import com.msm.core.metadata.ref.RefDataDefinition;

import java.util.List;

public enum BusinessProcessRefMetadata implements RefDataDefinition {
    TEAM_ID_REFERENCE(
            "id",
            "code",
            "name"
    )

    ;


    private final List<String> fields;

    BusinessProcessRefMetadata(String... fields) {
        this.fields = List.of(fields);
    }

    @Override
    public List<String> fields() {
        return fields;
    }
}

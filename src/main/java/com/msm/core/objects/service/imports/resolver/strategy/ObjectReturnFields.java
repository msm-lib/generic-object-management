package com.msm.core.objects.service.imports.resolver.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectReturnFields {
    private static final Map<String, List<String>> returnFields = new HashMap<>();


    static {
        returnFields.put("portaluser", List.of("id", "code", "username", "email"));
        returnFields.put("accountattribute", List.of("id", "code", "name", "type"));
        returnFields.put("account", List.of(
                "id",
                "code",
                "name",
                "email",
                "partyId",
                "partyNumber",
                "taxRegistration",
                "profileRelationshipType"
        ));
    }

    public static List<String> getReturnFields(String objectRefName) {
        List<String> fields = returnFields.get(objectRefName);
        if(fields == null){
            return List.of(
                    "id",
                    "code",
                    "name"
            );
        }
        return fields;
    }
}

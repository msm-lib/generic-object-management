package com.msm.core.objects.rules;

import com.msm.core.objects.exception.ObjectErrorDetail;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RuleResult {
    private final Map<String, Object> results = new HashMap<>();
    private final List<ObjectErrorDetail> errors = new ArrayList<>();

    public static RuleResult create() {
        return new RuleResult();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

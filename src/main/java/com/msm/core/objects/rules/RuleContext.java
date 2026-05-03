package com.msm.core.objects.rules;

import com.msm.core.objects.exception.ErrorDetail;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleContext<T> {
    private String groupName;
    private T source;
    private RuleResult ruleResult;
    public void addError(ErrorDetail msg) {
        this.ruleResult.getErrors().add(msg);
    }
}
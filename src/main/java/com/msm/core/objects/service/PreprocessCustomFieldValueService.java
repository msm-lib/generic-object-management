package com.msm.core.objects.service;

import com.msm.core.hook.anontation.Handler;
import com.msm.core.hook.context.ActionContext;
import com.msm.core.objects.ObjectConstants;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"unchecked"})
@RequiredArgsConstructor
public class PreprocessCustomFieldValueService{

    @Handler(action = ObjectConstants.UNWRAPPED_CUSTOM_VALUES)
    public <T> T handle(ActionContext<Map<String, Object>> actionRequest) {
        Map<String, Object> objectMap = actionRequest.getPayload();
        Optional<Map.Entry<String, Object>> objectCustomEntry =  objectMap
                .entrySet()
                .stream()
                .filter(entry -> ObjectConstants.CUSTOM_VALUE_FIELD_NAME.equals(entry.getKey()))
                .findAny();
        if(objectCustomEntry.isPresent()) {
            Map<String, Object> customFieldValues = (Map<String, Object>) objectCustomEntry.get().getValue();
            if(Objects.nonNull(customFieldValues)) {
                objectMap.putAll(customFieldValues);
                objectMap.remove(objectCustomEntry.get().getKey());
            }
        }
        return null;
    }
}
package com.msm.core.objects.hook.system;


import com.msm.core.commons.Constants;
import com.msm.core.hook.anontation.crud.HookAfterCommitCreate;
import com.msm.core.hook.anontation.crud.HookAfterCreate;
import com.msm.core.hook.context.ActionContext;
import com.msm.core.objects.service.ObjectUsageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class SystemHookEvent {
    private final ObjectUsageConfig objectUsageConfig;

    @HookAfterCreate(resource = Constants.GENERIC_SYSTEM_OBJECT)
    public void HookAfterCreate(ActionContext<Map<String, Object>> ctx) {
//        objectUsageService.saveObjectUsage(ctx);
//        log.info("[BEFORE_EVENT] Generic before create object: {}", ctx.getObjectId());
    }

    @HookAfterCommitCreate(resource = Constants.GENERIC_SYSTEM_OBJECT)
    public void HookAfterCommitCreate(ActionContext<Map<String, Object>> ctx) {
//        objectUsageService.sendEvent(ctx);
//        log.info("[BEFORE_EVENT] Generic before create object: {}", ctx.getObjectId());
    }
}

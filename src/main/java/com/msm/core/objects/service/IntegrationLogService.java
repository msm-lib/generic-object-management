package com.msm.core.objects.service;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.objects.entity.metadata.IntegrationLogMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RequiredArgsConstructor
public class IntegrationLogService {
    private final ActionExecutor actionExecutor;
//    private final ObjectQueryRepository internalObjectQueryRepository;


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIntegrationLog(Map<String, Object> integrationLog) {
//        internalObjectQueryRepository.save(IntegrationLogMeta.OBJECT_NAME, integrationLog);
        log(integrationLog);
    }

    private void log(Map<String, Object> integrationLog) {
        ActionContext<Map<String, Object>> actionContext = ActionContext
                .<Map<String, Object>>builder()
                .resource(IntegrationLogMeta.OBJECT_NAME)
                .action(Constants.Action.CREATE)
                .payload(integrationLog)
                .internalExecution(true)
                .build();

        actionExecutor.execute(actionContext);
    }

}

package com.msm.core.objects.service;

import com.msm.core.objects.entity.metadata.IntegrationLogMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RequiredArgsConstructor
public class IntegrationLogService {
    private final ObjectQueryRepository internalObjectQueryRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIntegrationLog(Map<String, Object> integrationLog) {
        internalObjectQueryRepository.save(IntegrationLogMeta.OBJECT_NAME, integrationLog);
    }
}

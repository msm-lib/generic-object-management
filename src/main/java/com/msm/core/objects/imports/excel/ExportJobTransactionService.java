package com.msm.core.objects.imports.excel;


import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ExportJobMeta;
import com.msm.core.objects.imports.model.ExportStatus;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RequiredArgsConstructor
public class ExportJobTransactionService {
    private final ObjectQueryRepository internalObjectQueryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExporting(DataRecord exportJobRecord) {
        exportJobRecord.set(ExportJobMeta.STATUS, ExportStatus.EXPORTING.name());
        internalObjectQueryRepository.update(
                ExportJobMeta.OBJECT_NAME,
                exportJobRecord.get(ExportJobMeta.ID),
                DataRecord.of().with(ExportJobMeta.STATUS, ExportStatus.EXPORTING.name()).getValues()
        );
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(DataRecord exportJobRecord) {
        internalObjectQueryRepository.update(
                ExportJobMeta.OBJECT_NAME,
                exportJobRecord.get(ExportJobMeta.ID),
                exportJobRecord.asMap()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(DataRecord exportJobRecord) {
        internalObjectQueryRepository.update(
                ExportJobMeta.OBJECT_NAME,
                exportJobRecord.get(ExportJobMeta.ID),
                exportJobRecord.asMap()
        );
    }

}

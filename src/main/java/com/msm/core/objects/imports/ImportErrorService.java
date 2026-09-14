package com.msm.core.objects.imports;

import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class ImportErrorService {
    private final ObjectQueryRepository internalObjectQueryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertErrors(List<DataRecord> errors) {
        internalObjectQueryRepository.insertBatch(
                ImportErrorMeta.OBJECT_NAME,
                Utils.D.toList(errors, DataRecord::getValues)
        );
    }
}

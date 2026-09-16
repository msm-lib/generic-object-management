package com.msm.core.objects.imports;

import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.imports.model.BatchImportResult;
import com.msm.core.objects.imports.model.ImportRowResult;
import com.msm.core.objects.imports.model.ImportStatus;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class BatchImportService {

    private final ImportErrorService importErrorService;
    private final ObjectQueryRepository internalObjectQueryRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchImportResult importBatch(
            UUID importId,
            String importObjectName,
            List<Map<String, Object>> rows
    ) {

        List<DataRecord> errors = new ArrayList<>();
        List<ImportRowResult> importRowResults = new ArrayList<>();
        long success = 0;

        for (Map<String, Object> row : rows) {
            DataRecord importStagingRecord = DataRecord.of(row);


            try {
                importOne(importObjectName, importStagingRecord.get(ImportStagingMeta.DATA));
                success++;
                importRowResults.add(
                        new ImportRowResult(
                                success,
                                ImportStatus.COMPLETED,
                                "COMPLETED",
                                row
                        )
                );
            } catch (Exception e) {
                errors.add(DataRecord.of()
                        .with(ImportErrorMeta.IMPORT_ID, importId)
                        .with(ImportErrorMeta.ROW_NUMBER, importStagingRecord.get(ImportStagingMeta.ROW_NUMBER))
                        .with(ImportErrorMeta.ERROR_TYPE, "DATABASE")
                        .with(ImportErrorMeta.ERROR_MESSAGE, e.getMessage())
                        .with(ImportErrorMeta.ERROR_CODE, "IMPORT_ERROR")
                        .with(ImportStagingMeta.DATA, row)
                );
                importRowResults.add(
                        new ImportRowResult(
                                success,
                                ImportStatus.COMPLETED_WITH_ERRORS,
                                "COMPLETED_WITH_ERRORS",
                                row
                        )
                );
            }
        }

        if (!errors.isEmpty()) {
            importErrorService.insertErrors(errors);
        }

        return new BatchImportResult(
                success,
                errors.size(),
                importRowResults
        );
    }


    private void importOne(
            String objectName,
            Map<String, Object> row
    ) {
        try {
            internalObjectQueryRepository.save(objectName, row);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Duplicate detected for object: {}, falling back to update. Error: {}", objectName, e.getMessage());
        }
    }
}

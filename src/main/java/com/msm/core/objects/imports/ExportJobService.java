package com.msm.core.objects.imports;

import com.msm.core.action.annotations.action.CreateHandler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.entity.metadata.AttachmentInfoMeta;
import com.msm.core.objects.entity.metadata.ExportJobMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.imports.model.ImportStatus;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ExportJobService {
    private static final String ATTACHMENT_OBJECT_NAME = "attachment";
    private static final String ATTACHMENT_ID_NAME = "attachmentId";
    private final GenericObjectInternalService genericObjectInternalService;
    private final ObjectQueryRepository internalObjectQueryRepository;

    @CreateHandler(resource = ImportJobMeta.OBJECT_NAME)
    public Map<String, Object> exportJob(ActionContext<Map<String, Object>> actionContext) {
        UUID importJobId = UUID.randomUUID();
        Map<String, Object> payload = actionContext.getPayload();
        DataRecord payloadRecord = DataRecord.of(payload);
        payloadRecord.put("sourceId", importJobId);
        payloadRecord.put("sourceType", ImportJobMeta.OBJECT_NAME);
        Map<String, Object> attachmentMap = genericObjectInternalService.createObject(ATTACHMENT_OBJECT_NAME, payloadRecord.getValues());
        DataRecord attachmentRecord = DataRecord.of(attachmentMap);


        DataRecord exportJobRecord = DataRecord.of()
                .with(ImportJobMeta.ID, importJobId)
                .with(ImportJobMeta.OBJECT_NAME_FIELD, payloadRecord.get(ImportJobMeta.OBJECT_NAME_FIELD))
                .with(ImportJobMeta.ATTACHMENT_ID, attachmentRecord.get(AttachmentInfoMeta.ID))
                .with(ImportJobMeta.FILE_NAME, attachmentRecord.get(AttachmentInfoMeta.ORIGINAL_FILE_NAME))
                .with(ImportJobMeta.FILE_PATH, attachmentRecord.get(AttachmentInfoMeta.S3_KEY))
                .with(ImportJobMeta.STATUS, ImportStatus.UPLOADED.name())
                .with(ImportJobMeta.STARTED_AT, Instant.now());

        DataRecord dataJobImport = DataRecord.of(internalObjectQueryRepository.save(ExportJobMeta.OBJECT_NAME, exportJobRecord.getValues()));
        dataJobImport.with(AttachmentInfoMeta.UPLOAD_URL, attachmentRecord.get(AttachmentInfoMeta.UPLOAD_URL));
        return dataJobImport.getValues();
    }
}

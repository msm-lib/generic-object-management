package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.annotations.action.CreateHandler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.connector.GenericObjectInternalService;
import com.msm.core.objects.dataexchange.imports.metadata.AttachmentInfoMeta;
import com.msm.core.objects.dataexchange.imports.metadata.ImportJobDtoMeta;
import com.msm.core.objects.dataexchange.imports.metadata.S3FileInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ImportJobService {
    private final GenericObjectInternalService genericObjectInternalService;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final S3FileUtils s3FileUtils;


//    @HookAfterFilter(resource = ImportJobMeta.OBJECT_NAME)
//    public void filterJob(ActionContext<ObjectFilterRequest> actionContext) {
//        PageResponse<Map<String, Object>> pageResponse = actionContext.getResultAs(new TypeReference<>() {});
//
//        Utils.CL.emptyIfNull(pageResponse.getContents()).forEach(object -> {
//
//        });
//    }

    @CreateHandler(resource = ImportJobMeta.OBJECT_NAME)
    public Map<String, Object> importJob(ActionContext<Map<String, Object>> actionContext) {

        //{
        //    "objectName": "profile",
        //    "service": "customer",
        //    "fileName": "Import_profile_25082026.xlsx",
        //    "originalFileName": "Import_profile_25082026.xlsx",
        //    "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        //    "contentLength": 240675,
        //    "description": ""
        //}
        // bhc/order/exportjob/20260919/file.xlsx

        //ImportJobDtoMeta

        UUID importJobId = UUID.randomUUID();
        Map<String, Object> payload = actionContext.getPayload();
        DataRecord payloadRecord = DataRecord.of(payload);

        String fileExtension = s3FileUtils.getFileExtension(payloadRecord.get(ImportJobDtoMeta.FILE_NAME));
        String s3FileName = s3FileUtils.generateFileName(importJobId, fileExtension);
        String s3Key = s3FileUtils.getS3Key(
                payloadRecord.get(ImportJobDtoMeta.OBJECT_NAME_FIELD),
                s3FileName
        );

        DataRecord s3FileInfoRecord = DataRecord
                .of()
                .with(S3FileInfoMeta.SERVICE, payloadRecord.get(ImportJobDtoMeta.SERVICE))
                .with(S3FileInfoMeta.S3_KEY, s3Key)
                .with(S3FileInfoMeta.S3_FILE_NAME, s3FileName)
                .with(S3FileInfoMeta.ORIGINAL_FILE_NAME, payloadRecord.get(ImportJobDtoMeta.ORIGINAL_FILE_NAME))
                .with(S3FileInfoMeta.FILE_TYPE, fileExtension)
                .with(S3FileInfoMeta.MIME_TYPE, payloadRecord.get(ImportJobDtoMeta.MIME_TYPE))
                .with(S3FileInfoMeta.CONTENT_LENGTH, payloadRecord.get(ImportJobDtoMeta.CONTENT_LENGTH));

        DataRecord importJobRecord = DataRecord.of()
                .with(ImportJobMeta.ID, importJobId)
                .with(ImportJobMeta.OBJECT_NAME_FIELD, payloadRecord.get(ImportJobMeta.OBJECT_NAME_FIELD))
                .with(ImportJobMeta.ORIGINAL_FILE_NAME, payloadRecord.get(ImportJobDtoMeta.ORIGINAL_FILE_NAME))
                .with(ImportJobMeta.FILE_NAME, s3FileName)
                .with(ImportJobMeta.STATUS, ImportStatus.UPLOADING.name())
                .with(ImportJobMeta.FILE_INFO, s3FileInfoRecord.getValues())
                .with(ImportJobMeta.STATUS, ImportStatus.UPLOADING.name())
                .with(ImportJobMeta.STARTED_AT, Instant.now());

        DataRecord dataJobImport = DataRecord.of(internalObjectQueryRepository.save(ImportJobMeta.OBJECT_NAME, importJobRecord.getValues()));

        String uploadUrl = s3FileUtils.getUploadUrl(
                s3Key,
                payloadRecord.get(ImportJobDtoMeta.MIME_TYPE),
                payloadRecord.get(ImportJobDtoMeta.CONTENT_LENGTH)
        );


        dataJobImport.with(AttachmentInfoMeta.FILE_NAME, payloadRecord.get(ImportJobDtoMeta.ORIGINAL_FILE_NAME));
        dataJobImport.with(AttachmentInfoMeta.UPLOAD_URL, uploadUrl);
        dataJobImport.remove(ImportJobMeta.FILE_INFO);

        return dataJobImport.getValues();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobValidating(UUID importId) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                importId,
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATING.name()).getValues()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobImporting(UUID importId) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                importId,
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.IMPORTING.name()).getValues()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobValidateFailed(UUID importId) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                importId,
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATION_FAILED.name()).getValues()
        );
    }
}

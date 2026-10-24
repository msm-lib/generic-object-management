package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.annotations.action.CreateHandler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.dataexchange.imports.metadata.AttachmentInfoMeta;
import com.msm.core.objects.dataexchange.imports.metadata.ImportJobDtoMeta;
import com.msm.core.objects.dataexchange.imports.metadata.S3FileInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ImportJobService {
    public static final String PARENT_IMPORT_JOB_ID = "parentImportJobId";
    /** {@code fileInfo.sheets}: one summary per sheet of a workbook import, see ImportFileService. */
    public static final String SHEETS = "sheets";
    public static final String RUN_ID = "runId";
    /** {@code fileInfo.context}: the header a job imports details into, see {@link ImportContext}. */
    public static final String CONTEXT = "context";

    private final ObjectQueryRepository internalObjectQueryRepository;
    private final S3FileUtils s3FileUtils;
    /**
     * Sheet runs in progress, by run id. The steps of a sheet run use the run id as import id, so its staging and error
     * rows stay apart from the other sheets; counters and status go to the job.
     */
    private final Map<UUID, SheetRun> sheetRuns = new ConcurrentHashMap<>();

    /** A sheet run: its workbook's job and the job's error rows when the run started (the errors of earlier sheets). */
    private record SheetRun(UUID jobId, long errorRowsBefore) { }



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

    /** Starts a run of another sheet of the job's workbook; the run id is the import id of that sheet's steps. */
    public UUID startSheetRun(UUID jobId) {
        UUID runId = UUID.randomUUID();
        Object errorRows = Utils.CL.emptyIfNull(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId))
                .get(ImportJobMeta.ERROR_ROWS.getFieldName());
        sheetRuns.put(runId, new SheetRun(jobId, errorRows instanceof Number number ? number.longValue() : 0L));
        return runId;
    }

    public void endSheetRun(UUID runId) {
        sheetRuns.remove(runId);
    }

    /** The job an import id belongs to: the workbook's job for a sheet run, the id itself otherwise. */
    public UUID jobIdOf(UUID importId) {
        SheetRun run = importId == null ? null : sheetRuns.get(importId);
        return run == null ? importId : run.jobId();
    }

    /**
     * The job's error rows that are not this import id's: those of the earlier sheets for a sheet run, none otherwise.
     * A step has errors of its own when the job's error rows exceed this.
     */
    public long errorRowsBefore(UUID importId) {
        SheetRun run = importId == null ? null : sheetRuns.get(importId);
        return run == null ? 0L : run.errorRowsBefore();
    }

    /** The header the job of an import id writes details into, or null when it imports rows on their own. */
    public ImportContext contextOf(UUID importId) {
        if (importId == null) {
            return null;
        }
        Map<String, Object> job = internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobIdOf(importId));
        Map<String, Object> fileInfo = Utils.CL.emptyIfNull(DataRecord.ofNullable(job).get(ImportJobMeta.FILE_INFO));
        return ImportContext.from(fileInfo.get(CONTEXT));
    }

    /** Stores the header of the job in {@code fileInfo.context}; committed on its own so it stays when the import fails. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveContext(UUID jobId, ImportContext context) {
        Map<String, Object> job = internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
        Map<String, Object> fileInfo = new HashMap<>(Utils.CL.emptyIfNull(DataRecord.ofNullable(job).get(ImportJobMeta.FILE_INFO)));
        fileInfo.put(CONTEXT, context.toMap());
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                jobId,
                DataRecord.of().with(ImportJobMeta.FILE_INFO, fileInfo).getValues()
        );
    }

    /**
     * Creates the job of another sheet of the parent's workbook: same uploaded file, different object.
     * The parent job id is kept in {@code fileInfo.parentImportJobId}.
     *
     * @deprecated a workbook is one job; its other sheets run with {@link #startSheetRun}
     */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> createSheetJob(Map<String, Object> parentJob, String objectName) {
        DataRecord parentRecord = DataRecord.of(parentJob);
        Map<String, Object> fileInfo = new HashMap<>(Utils.CL.emptyIfNull(parentRecord.get(ImportJobMeta.FILE_INFO)));
        fileInfo.put(PARENT_IMPORT_JOB_ID, parentRecord.get(ImportJobMeta.ID));

        DataRecord sheetJobRecord = DataRecord.of()
                .with(ImportJobMeta.ID, UUID.randomUUID())
                .with(ImportJobMeta.OBJECT_NAME_FIELD, objectName)
                .with(ImportJobMeta.ORIGINAL_FILE_NAME, parentRecord.get(ImportJobMeta.ORIGINAL_FILE_NAME))
                .with(ImportJobMeta.FILE_NAME, parentRecord.get(ImportJobMeta.FILE_NAME))
                .with(ImportJobMeta.STATUS, ImportStatus.UPLOADING.name())
                .with(ImportJobMeta.FILE_INFO, fileInfo)
                .with(ImportJobMeta.STARTED_AT, Instant.now());
        return internalObjectQueryRepository.save(ImportJobMeta.OBJECT_NAME, sheetJobRecord.getValues());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobValidating(UUID importId) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                jobIdOf(importId),
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.VALIDATING.name()).getValues()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobImporting(UUID importId) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                jobIdOf(importId),
                DataRecord.of().with(ImportJobMeta.STATUS, ImportStatus.IMPORTING.name()).getValues()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobValidateFailed(UUID importId, String errorMessage) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                jobIdOf(importId),
                DataRecord
                        .of()
                        .with(ImportJobMeta.STATUS, ImportStatus.VALIDATION_FAILED.name())
                        .with(ImportJobMeta.ERROR_MESSAGE, errorMessage)
                        .getValues()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void makeJobFailed(UUID importId, String errorMessage) {
        internalObjectQueryRepository.update(
                ImportJobMeta.OBJECT_NAME,
                jobIdOf(importId),
                DataRecord
                        .of()
                        .with(ImportJobMeta.STATUS, ImportStatus.FAILED.name())
                        .with(ImportJobMeta.ERROR_MESSAGE, errorMessage)
                        .with(ImportJobMeta.COMPLETED_AT, Instant.now())
                        .getValues()
        );
    }
}

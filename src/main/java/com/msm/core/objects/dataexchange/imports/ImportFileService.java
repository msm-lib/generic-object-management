package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.dataexchange.imports.csv.FileReaderService;
import com.msm.core.objects.dataexchange.imports.metadata.AttachmentInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.ObjectImportContext;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entry point of the import API: validates the uploaded file, then imports it.
 * <ul>
 *   <li>Job {@code UPLOADING}: validate, then import if the validation is {@code VALIDATED}. A validation with
 *       errors leaves the job {@code VALIDATION_FAILED} and nothing is imported.</li>
 *   <li>Job {@code VALIDATED} (validated by the deprecated validate API): import only.</li>
 *   <li>Any other status: rejected, the job was already processed.</li>
 * </ul>
 * Excel only: once the object's sheet is {@code COMPLETED}, the sheets of the objects that depend on it
 * ({@link ImportConfigService#getWorkbookObjects}) are processed the same way, one after another, each in its own job
 * on the same file. A sheet must be imported before the next one is validated, because a child sheet looks up its
 * parent rows (by code) in the database. The first sheet that is not {@code COMPLETED} stops the run; a dependent
 * sheet missing from the file is skipped.
 * <p>
 * Response: the object's job, plus {@code sheetJobs} (one summary per dependent sheet) when there are dependents.
 */
@Slf4j
@RequiredArgsConstructor
public class ImportFileService {
    public static final String SHEET_JOBS = "sheetJobs";
    public static final String SKIPPED = "SKIPPED";

    private static final String IMPORT_JOB_ID_NAME = "importJobId";
    private static final String SHEET = "sheet";
    private static final String MESSAGE = "message";

    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ActionExecutor actionExecutor;
    private final ImportConfigService importConfigService;
    private final ImportJobService importJobService;
    private final FileReaderService fileReaderService;
    private final S3FileUtils s3FileUtils;

    private record Steps(String validation, String importData) {
        static final Steps EXCEL = new Steps(ObjectActionNamed.Excel.VALIDATION, ObjectActionNamed.Excel.IMPORT_DATA);
        static final Steps CSV = new Steps(ObjectActionNamed.Csv.VALIDATION, ObjectActionNamed.Csv.IMPORT_DATA);
    }

    @Handler(action = ObjectActionNamed.Excel.IMPORT_FILE)
    public Map<String, Object> importExcel(ActionContext<ObjectImportContext> actionContext) {
        ObjectImportContext context = actionContext.getPayload();
        String objectName = context.importObjectName();

        Map<String, Object> job = importObject(objectName, getJob(objectName, context.jobId()), context.param(), Steps.EXCEL);

        List<String> dependents = importConfigService.getWorkbookObjects(objectName);
        dependents = dependents.subList(1, dependents.size());
        if (dependents.isEmpty()) {
            return job;
        }

        Set<String> sheetNames = getSheetNames(objectName, job);
        boolean stopped = !ImportStatus.COMPLETED.name().equals(DataRecord.of(job).get(ImportJobMeta.STATUS));
        List<Map<String, Object>> sheetJobs = new ArrayList<>();
        for (String dependent : dependents) {
            String sheet = importConfigService.getSheet(dependent);
            // a single-sheet file is read as the object's own sheet whatever its name (FileReaderService.getSheet)
            if (sheetNames.size() < 2 || !sheetNames.contains(sheet.toLowerCase(Locale.ROOT))) {
                sheetJobs.add(skipped(dependent, sheet, "Sheet not found in the file"));
            } else if (stopped) {
                sheetJobs.add(skipped(dependent, sheet, "Skipped because a previous sheet is not completed"));
            } else {
                Map<String, Object> sheetJob = importSheet(dependent, importJobService.createSheetJob(job, dependent), context.param());
                sheetJobs.add(summary(sheetJob, sheet));
                stopped = !ImportStatus.COMPLETED.name().equals(DataRecord.of(sheetJob).get(ImportJobMeta.STATUS));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>(job);
        response.put(SHEET_JOBS, sheetJobs);
        return response;
    }

    @Handler(action = ObjectActionNamed.Csv.IMPORT_FILE)
    public Map<String, Object> importCsv(ActionContext<ObjectImportContext> actionContext) {
        ObjectImportContext context = actionContext.getPayload();
        String objectName = context.importObjectName();
        return importObject(objectName, getJob(objectName, context.jobId()), context.param(), Steps.CSV);
    }

    /** Validates (unless already validated) and imports one job. Errors propagate, like the validate/import APIs. */
    private Map<String, Object> importObject(String objectName, Map<String, Object> job, Map<String, Object> param, Steps steps) {
        UUID jobId = DataRecord.of(job).get(ImportJobMeta.ID);
        String status = DataRecord.of(job).get(ImportJobMeta.STATUS);

        if (ImportStatus.UPLOADING.name().equals(status)) {
            job = actionExecutor.execute(ActionContext
                    .<Map<String, Object>>builder()
                    .resource(objectName)
                    .action(steps.validation())
                    .payload(new HashMap<>(Map.of(IMPORT_JOB_ID_NAME, jobId)))
                    .build());
            status = DataRecord.of(job).get(ImportJobMeta.STATUS);
        } else if (!ImportStatus.VALIDATED.name().equals(status)) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import job " + jobId + " was already processed (status " + status + ")", null);
        }

        if (!ImportStatus.VALIDATED.name().equals(status)) {
            return job;
        }
        return actionExecutor.execute(ActionContext
                .<ObjectImportContext>builder()
                .resource(objectName)
                .action(steps.importData())
                .payload(ObjectImportContext.of(objectName, jobId, param))
                .build());
    }

    /** Like {@link #importObject} for a dependent sheet, but a failure is reported in its job instead of thrown. */
    private Map<String, Object> importSheet(String objectName, Map<String, Object> sheetJob, Map<String, Object> param) {
        UUID jobId = DataRecord.of(sheetJob).get(ImportJobMeta.ID);
        try {
            return importObject(objectName, sheetJob, param, Steps.EXCEL);
        } catch (Exception e) {
            // the validate/import handlers already marked the job VALIDATION_FAILED / FAILED
            log.error("Import of sheet object [{}] job [{}] failed", objectName, jobId, e);
            return internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
        }
    }

    private Map<String, Object> getJob(String objectName, UUID jobId) {
        Map<String, Object> job = internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
        if (job == null) {
            throw ObjectErrors.notFound(ImportJobMeta.OBJECT_NAME);
        }
        if (!objectName.equals(DataRecord.of(job).get(ImportJobMeta.OBJECT_NAME_FIELD))) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import job " + jobId + " does not belong to object " + objectName, null);
        }
        return job;
    }

    private Set<String> getSheetNames(String objectName, Map<String, Object> job) {
        String fileUrl = DataRecord.of(s3FileUtils.getDownloadInfo(job)).get(AttachmentInfoMeta.DOWNLOAD_URL);
        int bufferSize = importConfigService.getProcessingConfig(objectName).getBufferSize();
        // lower-cased: POI matches sheet names ignoring case, see FileReaderService.getSheet
        Set<String> sheetNames = new HashSet<>();
        fileReaderService.getSheetNames(fileUrl, bufferSize).forEach(name -> sheetNames.add(name.toLowerCase(Locale.ROOT)));
        return sheetNames;
    }

    private static Map<String, Object> summary(Map<String, Object> job, String sheet) {
        DataRecord jobRecord = DataRecord.of(job);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), jobRecord.get(ImportJobMeta.OBJECT_NAME_FIELD));
        summary.put(SHEET, sheet);
        summary.put(IMPORT_JOB_ID_NAME, jobRecord.get(ImportJobMeta.ID));
        summary.put(ImportJobMeta.STATUS.getFieldName(), jobRecord.get(ImportJobMeta.STATUS));
        summary.put(ImportJobMeta.TOTAL_ROWS.getFieldName(), jobRecord.get(ImportJobMeta.TOTAL_ROWS));
        summary.put(ImportJobMeta.VALID_ROWS.getFieldName(), jobRecord.get(ImportJobMeta.VALID_ROWS));
        summary.put(ImportJobMeta.SUCCESS_ROWS.getFieldName(), jobRecord.get(ImportJobMeta.SUCCESS_ROWS));
        summary.put(ImportJobMeta.ERROR_ROWS.getFieldName(), jobRecord.get(ImportJobMeta.ERROR_ROWS));
        summary.put(ImportJobMeta.ERROR_MESSAGE.getFieldName(), jobRecord.get(ImportJobMeta.ERROR_MESSAGE));
        return summary;
    }

    private static Map<String, Object> skipped(String objectName, String sheet, String message) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), objectName);
        summary.put(SHEET, sheet);
        summary.put(ImportJobMeta.STATUS.getFieldName(), SKIPPED);
        summary.put(MESSAGE, message);
        return summary;
    }
}

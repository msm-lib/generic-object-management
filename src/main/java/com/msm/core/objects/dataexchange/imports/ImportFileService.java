package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Utils;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.metadata.typesafe.TypedAttribute;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
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

import java.time.Instant;
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
 * ({@link ImportConfigService#getWorkbookObjects}) are processed the same way, one after another, in the <b>same job</b>:
 * each sheet is a run ({@link ImportJobService#startSheetRun}) whose id keys its staging and error rows, while the
 * counters and the status are the job's. A sheet must be imported before the next one is validated, because a child
 * sheet looks up its parent rows (by code) in the database. A sheet whose validation or import fails
 * ({@code VALIDATION_FAILED}, {@code FAILED}) stops the run; a sheet imported with rows in error
 * ({@code COMPLETED_WITH_ERRORS}) does not. A dependent sheet missing from the file is skipped. Once the root sheet is
 * imported, a dependent sheet with errors leaves the job {@code COMPLETED_WITH_ERRORS}, its error message listing the
 * sheets.
 * <p>
 * The job keeps one summary per sheet in {@code fileInfo.sheets} ({@code objectName}, {@code sheet}, {@code runId},
 * status, row counts, error message). Response: the job, plus {@code sheets} when there are dependents
 * ({@code sheetJobs}, the dependent sheets only, is kept for older clients).
 */
@Slf4j
@RequiredArgsConstructor
public class ImportFileService {
    /** @deprecated the dependent sheets of {@link #SHEETS}, kept for clients of the job-per-sheet import */
    @Deprecated
    public static final String SHEET_JOBS = "sheetJobs";
    public static final String SHEETS = ImportJobService.SHEETS;
    public static final String SKIPPED = "SKIPPED";

    private static final String IMPORT_JOB_ID_NAME = "importJobId";
    private static final String SHEET = "sheet";
    private static final String MESSAGE = "message";
    private static final List<TypedAttribute<Long>> COUNTERS = List.of(
            ImportJobMeta.TOTAL_ROWS, ImportJobMeta.VALID_ROWS, ImportJobMeta.SUCCESS_ROWS, ImportJobMeta.ERROR_ROWS);

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

        UUID jobId = DataRecord.of(job).get(ImportJobMeta.ID);
        Set<String> sheetNames = getSheetNames(objectName, job);
        List<Map<String, Object>> sheets = new ArrayList<>();
        sheets.add(sheetSummary(objectName, importConfigService.getSheet(objectName), jobId, Map.of(), job));
        boolean stopped = !canContinue(DataRecord.of(job).get(ImportJobMeta.STATUS));
        for (String dependent : dependents) {
            String sheet = importConfigService.getSheet(dependent);
            // a single-sheet file is read as the object's own sheet whatever its name (FileReaderService.getSheet)
            if (sheetNames.size() < 2 || !sheetNames.contains(sheet.toLowerCase(Locale.ROOT))) {
                sheets.add(skipped(dependent, sheet, "Sheet not found in the file"));
            } else if (stopped) {
                sheets.add(skipped(dependent, sheet, "Skipped because a previous sheet is not completed"));
            } else {
                Map<String, Object> before = internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
                UUID runId = importJobService.startSheetRun(jobId);
                Map<String, Object> after;
                try {
                    after = runSheet(dependent, runId, context.param());
                } finally {
                    importJobService.endSheetRun(runId);
                }
                Map<String, Object> summary = sheetSummary(dependent, sheet, runId, before, after);
                sheets.add(summary);
                String status = DataRecord.of(after).get(ImportJobMeta.STATUS);
                stopped = !canContinue(status);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>(finishWorkbook(jobId, sheets));
        response.put(SHEETS, sheets);
        response.put(SHEET_JOBS, sheets.subList(1, sheets.size()).stream().map(sheetSummary -> legacySheetJob(sheetSummary, jobId)).toList());
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

    /**
     * Validates then imports a dependent sheet as a run of the workbook's job: the run id is the import id of both steps.
     * Returns the job; a failure is reported in the job (the steps marked it VALIDATION_FAILED / FAILED) instead of thrown.
     */
    private Map<String, Object> runSheet(String objectName, UUID runId, Map<String, Object> param) {
        try {
            Map<String, Object> job = actionExecutor.execute(ActionContext
                    .<Map<String, Object>>builder()
                    .resource(objectName)
                    .action(Steps.EXCEL.validation())
                    .payload(new HashMap<>(Map.of(IMPORT_JOB_ID_NAME, runId)))
                    .build());
            if (!ImportStatus.VALIDATED.name().equals(DataRecord.of(job).get(ImportJobMeta.STATUS))) {
                return job;
            }
            return actionExecutor.execute(ActionContext
                    .<ObjectImportContext>builder()
                    .resource(objectName)
                    .action(Steps.EXCEL.importData())
                    .payload(ObjectImportContext.of(objectName, runId, param))
                    .build());
        } catch (Exception e) {
            log.error("Import of sheet object [{}] run [{}] failed", objectName, runId, e);
            return internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, importJobService.jobIdOf(runId));
        }
    }

    /**
     * Stores the sheet summaries in {@code fileInfo.sheets} and sets the status of the workbook: when the root sheet was
     * imported, any sheet with errors (rows in error, or a dependent sheet whose validation or import failed) leaves the
     * job {@code COMPLETED_WITH_ERRORS}, its error message listing those sheets. A root sheet that was not imported keeps
     * its own status.
     */
    private Map<String, Object> finishWorkbook(UUID jobId, List<Map<String, Object>> sheets) {
        Map<String, Object> job = internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
        Map<String, Object> fileInfo = new HashMap<>(Utils.CL.emptyIfNull(DataRecord.of(job).get(ImportJobMeta.FILE_INFO)));
        fileInfo.put(SHEETS, sheets);
        DataRecord values = DataRecord.of().with(ImportJobMeta.FILE_INFO, fileInfo);
        String rootStatus = (String) sheets.getFirst().get(ImportJobMeta.STATUS.getFieldName());
        List<String> sheetErrors = sheets.stream()
                .filter(sheet -> hasErrors((String) sheet.get(ImportJobMeta.STATUS.getFieldName())))
                .map(sheet -> sheet.get(SHEET) + ": " + errorMessage(sheet))
                .toList();
        if (canContinue(rootStatus) && !sheetErrors.isEmpty()) {
            values.with(ImportJobMeta.STATUS, ImportStatus.COMPLETED_WITH_ERRORS.name())
                    .with(ImportJobMeta.ERROR_MESSAGE, String.join("; ", sheetErrors))
                    .with(ImportJobMeta.COMPLETED_AT, Instant.now());
        }
        internalObjectQueryRepository.update(ImportJobMeta.OBJECT_NAME, jobId, values.getValues());
        return internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId);
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
        ObjectImportRegistry.ParentConfig parent = importConfigService.getParent(objectName);
        if (parent != null && parent.isRequired() && importJobService.contextOf(jobId) == null) {
            // rows of this object are imported only into a header, through PUT /{header}/{id}/import_details
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import of " + objectName + " is done from a " + parent.getObject() + " (action "
                            + ObjectActionNamed.ImportDetails.IMPORT_DETAILS + ")", null);
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

    /** Summary of one sheet run: the status the run left the job in, and the job's counters it added. */
    private static Map<String, Object> sheetSummary(String objectName, String sheet, UUID runId,
                                                    Map<String, Object> before, Map<String, Object> after) {
        DataRecord jobRecord = DataRecord.of(after);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), objectName);
        summary.put(SHEET, sheet);
        summary.put(ImportJobService.RUN_ID, runId);
        summary.put(ImportJobMeta.STATUS.getFieldName(), jobRecord.get(ImportJobMeta.STATUS));
        for (TypedAttribute<Long> counter : COUNTERS) {
            summary.put(counter.getFieldName(), count(after, counter) - count(before, counter));
        }
        summary.put(ImportJobMeta.ERROR_MESSAGE.getFieldName(), jobRecord.get(ImportJobMeta.ERROR_MESSAGE));
        return summary;
    }

    private static long count(Map<String, Object> job, TypedAttribute<Long> counter) {
        Object value = Utils.CL.emptyIfNull(job).get(counter.getFieldName());
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * A sheet imported, with or without rows in error, lets the next sheets run; a failed validation or import stops them.
     */
    private static boolean canContinue(String status) {
        return ImportStatus.COMPLETED.name().equals(status) || ImportStatus.COMPLETED_WITH_ERRORS.name().equals(status);
    }

    private static boolean hasErrors(String status) {
        return ImportStatus.COMPLETED_WITH_ERRORS.name().equals(status)
                || ImportStatus.VALIDATION_FAILED.name().equals(status)
                || ImportStatus.FAILED.name().equals(status);
    }

    private static String errorMessage(Map<String, Object> summary) {
        Object message = summary.get(ImportJobMeta.ERROR_MESSAGE.getFieldName());
        if (message != null && Utils.STR.isNotBlank(message.toString())) {
            return message.toString();
        }
        return summary.get(ImportJobMeta.ERROR_ROWS.getFieldName()) + " row(s) with errors";
    }

    /** A sheet summary in the shape of the former sheet job; importJobId is the workbook's job. */
    private static Map<String, Object> legacySheetJob(Map<String, Object> sheetSummary, UUID jobId) {
        Map<String, Object> legacy = new LinkedHashMap<>(sheetSummary);
        legacy.remove(ImportJobService.RUN_ID);
        legacy.put(IMPORT_JOB_ID_NAME, jobId);
        return legacy;
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

package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.exceptions.GenericBaseException;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.imports.csv.FileReaderService;
import com.msm.core.objects.dataexchange.imports.metadata.AttachmentInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.ObjectImportContext;
import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportFileServiceTest {

    private static final String STATUS = ImportJobMeta.STATUS.getFieldName();
    private static final String TOTAL = ImportJobMeta.TOTAL_ROWS.getFieldName();
    private static final String SUCCESS = ImportJobMeta.SUCCESS_ROWS.getFieldName();
    private static final String VALIDATE = ObjectActionNamed.Excel.VALIDATION;
    private static final String IMPORT = ObjectActionNamed.Excel.IMPORT_DATA;
    private static final Map<String, Long> ROWS = Map.of("v2order", 2L, "v2orderitem", 3L, "product", 1L);

    private final Map<UUID, Map<String, Object>> jobs = new HashMap<>();
    private final Map<UUID, UUID> runs = new HashMap<>();
    private final List<String> executed = new ArrayList<>();
    private final List<UUID> importIds = new ArrayList<>();
    // status each step leaves the job of an object in; default VALIDATED / COMPLETED
    private final Map<String, String> validationStatus = new HashMap<>();
    private final Map<String, String> importStatus = new HashMap<>();

    private final ObjectQueryRepository repository = mock(ObjectQueryRepository.class);
    private final ActionExecutor actionExecutor = mock(ActionExecutor.class);
    private final ImportJobService importJobService = mock(ImportJobService.class);
    private final FileReaderService fileReaderService = mock(FileReaderService.class);
    private final S3FileUtils s3FileUtils = mock(S3FileUtils.class);
    private ImportFileService service;

    @BeforeEach
    void setUp() {
        ObjectImportRegistry registry = new Binder(new MapConfigurationPropertySource(Map.of(
                "imports.processing.buffer-size", "1024",
                "imports.objects.v2order.source.sheet", "Order",
                "imports.objects.v2orderitem.source.sheet", "OrderItem",
                "imports.objects.v2orderitem.depends-on[0]", "v2order"
        ))).bindOrCreate("imports", ObjectImportRegistry.class);
        service = new ImportFileService(repository, actionExecutor, new ImportConfigService(registry),
                importJobService, fileReaderService, s3FileUtils);

        // a copy, like a read from the database
        when(repository.findById(eq(ImportJobMeta.OBJECT_NAME), any())).thenAnswer(inv -> {
            Map<String, Object> job = jobs.get((UUID) inv.getArgument(1));
            return job == null ? null : new HashMap<>(job);
        });
        when(s3FileUtils.getDownloadInfo(anyMap())).thenReturn(Map.of(AttachmentInfoMeta.DOWNLOAD_URL.getFieldName(), "http://file"));
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("Order", "OrderItem"));
        when(importJobService.startSheetRun(any())).thenAnswer(inv -> {
            UUID runId = UUID.randomUUID();
            runs.put(runId, inv.getArgument(0));
            return runId;
        });
        when(importJobService.jobIdOf(any())).thenAnswer(inv -> runs.getOrDefault((UUID) inv.getArgument(0), inv.getArgument(0)));
        when(repository.update(eq(ImportJobMeta.OBJECT_NAME), any(), anyMap())).thenAnswer(inv -> {
            jobs.get((UUID) inv.getArgument(1)).putAll(inv.getArgument(2));
            return 1;
        });
        when(actionExecutor.execute(any())).thenAnswer(inv -> {
            ActionContext<?> ctx = inv.getArgument(0);
            String step = ctx.getAction().equals(VALIDATE) || ctx.getAction().equals(ObjectActionNamed.Csv.VALIDATION) ? "validate" : "import";
            executed.add(ctx.getResource() + ":" + step);
            if (step.equals("validate")) {
                UUID importId = (UUID) ((Map<?, ?>) ctx.getPayload()).get("importJobId");
                importIds.add(importId);
                String status = validationStatus.getOrDefault(ctx.getResource(), ImportStatus.VALIDATED.name());
                // every row of the sheet is counted; a failed validation has one row in error
                addCounters(importId, ROWS.get(ctx.getResource()), ImportStatus.VALIDATED.name().equals(status) ? 0 : 1, 0);
                return setStatus(importId, status);
            }
            UUID importId = ((ObjectImportContext) ctx.getPayload()).jobId();
            importIds.add(importId);
            addCounters(importId, 0, 0, ROWS.get(ctx.getResource()));
            return setStatus(importId, importStatus.getOrDefault(ctx.getResource(), ImportStatus.COMPLETED.name()));
        });
    }

    @Test
    void validatesThenImportsOrderSheetThenOrderItemSheetInOneJob() {
        Map<String, Object> job = newJob("v2order", ImportStatus.UPLOADING);
        UUID jobId = (UUID) job.get(ImportJobMeta.ID.getFieldName());

        Map<String, Object> result = importExcel("v2order", job);

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate", "v2orderitem:import");
        verify(importJobService, never()).createSheetJob(anyMap(), anyString());
        assertThat(jobs).hasSize(1);
        // the order sheet runs with the job id, the item sheet with its own run id
        UUID itemRunId = importIds.get(2);
        assertThat(importIds).containsExactly(jobId, jobId, itemRunId, itemRunId);
        assertThat(itemRunId).isNotEqualTo(jobId);
        verify(importJobService).endSheetRun(itemRunId);

        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED.name());
        assertThat(result.get(TOTAL)).isEqualTo(5L);
        assertThat(result.get(SUCCESS)).isEqualTo(5L);
        List<Map<String, Object>> sheets = sheets(result);
        assertThat(sheets).extracting(sheet -> sheet.get("sheet")).containsExactly("Order", "OrderItem");
        assertThat(sheets).extracting(sheet -> sheet.get(ImportJobService.RUN_ID)).containsExactly(jobId, itemRunId);
        assertThat(sheets).extracting(sheet -> sheet.get(TOTAL)).containsExactly(2L, 3L);
        assertThat(sheets).extracting(sheet -> sheet.get(SUCCESS)).containsExactly(2L, 3L);
        assertThat(storedSheets(jobId)).isEqualTo(sheets);
        assertThat(sheetJobStatuses(result)).containsExactly(ImportStatus.COMPLETED.name());
    }

    @Test
    void validationErrorsImportNothing() {
        validationStatus.put("v2order", ImportStatus.VALIDATION_FAILED.name());

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate");
        verify(importJobService, never()).startSheetRun(any());
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.VALIDATION_FAILED.name());
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.VALIDATION_FAILED.name(), ImportFileService.SKIPPED);
    }

    @Test
    void orderItemValidationErrorsLeaveTheJobCompletedWithErrors() {
        validationStatus.put("v2orderitem", ImportStatus.VALIDATION_FAILED.name());

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate");
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED_WITH_ERRORS.name());
        assertThat(result.get(ImportJobMeta.ERROR_MESSAGE.getFieldName())).isEqualTo("OrderItem: 1 row(s) with errors");
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.COMPLETED.name(), ImportStatus.VALIDATION_FAILED.name());
        assertThat(sheets(result).get(1).get(ImportJobMeta.ERROR_ROWS.getFieldName())).isEqualTo(1L);
    }

    @Test
    void failingOrderItemStepIsReportedInTheJob() {
        doAnswer(inv -> {
            ActionContext<?> ctx = inv.getArgument(0);
            executed.add(ctx.getResource() + ":" + (ctx.getAction().equals(VALIDATE) ? "validate" : "import"));
            if (ctx.getResource().equals("v2orderitem")) {
                UUID runId = (UUID) ((Map<?, ?>) ctx.getPayload()).get("importJobId");
                setStatus(runId, ImportStatus.VALIDATION_FAILED.name());
                jobs.get(runs.get(runId)).put(ImportJobMeta.ERROR_MESSAGE.getFieldName(), "Excel file not found");
                throw new IllegalStateException("boom");
            }
            UUID importId = ctx.getPayload() instanceof ObjectImportContext c ? c.jobId() : (UUID) ((Map<?, ?>) ctx.getPayload()).get("importJobId");
            return setStatus(importId, ctx.getAction().equals(VALIDATE) ? ImportStatus.VALIDATED.name() : ImportStatus.COMPLETED.name());
        }).when(actionExecutor).execute(any());

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED_WITH_ERRORS.name());
        assertThat(result.get(ImportJobMeta.ERROR_MESSAGE.getFieldName())).isEqualTo("OrderItem: Excel file not found");
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.COMPLETED.name(), ImportStatus.VALIDATION_FAILED.name());
        verify(importJobService).endSheetRun(any());
    }

    @Test
    void dependentSheetNameMatchesIgnoringCase() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("order", "orderitem"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate", "v2orderitem:import");
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.COMPLETED.name(), ImportStatus.COMPLETED.name());
    }

    @Test
    void missingOrderItemSheetIsSkipped() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("Order"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import");
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED.name());
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.COMPLETED.name(), ImportFileService.SKIPPED);
    }

    @Test
    void singleSheetFileIsOnlyTheRootSheetEvenWhenNamedLikeADependent() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("OrderItem"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import");
        assertThat(sheetStatuses(result)).containsExactly(ImportStatus.COMPLETED.name(), ImportFileService.SKIPPED);
    }

    @Test
    void jobValidatedByTheOldValidateApiIsOnlyImported() {
        Map<String, Object> result = importExcel("v2orderitem", newJob("v2orderitem", ImportStatus.VALIDATED));

        assertThat(executed).containsExactly("v2orderitem:import");
        assertThat(result).doesNotContainKey(ImportFileService.SHEET_JOBS).doesNotContainKey(ImportFileService.SHEETS);
    }

    @Test
    void processedJobIsRejected() {
        Map<String, Object> job = newJob("v2order", ImportStatus.COMPLETED);

        assertThatThrownBy(() -> importExcel("v2order", job)).isInstanceOf(GenericBaseException.class);
        assertThat(executed).isEmpty();
    }

    @Test
    void csvValidatesThenImports() {
        Map<String, Object> job = newJob("product", ImportStatus.UPLOADING);

        Map<String, Object> result = service.importCsv(context(ObjectActionNamed.Csv.IMPORT_FILE, "product", job));

        assertThat(executed).containsExactly("product:validate", "product:import");
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED.name());
    }

    private Map<String, Object> importExcel(String objectName, Map<String, Object> job) {
        return service.importExcel(context(ObjectActionNamed.Excel.IMPORT_FILE, objectName, job));
    }

    private static ActionContext<ObjectImportContext> context(String action, String objectName, Map<String, Object> job) {
        UUID jobId = (UUID) job.get(ImportJobMeta.ID.getFieldName());
        return ActionContext.<ObjectImportContext>builder()
                .resource(objectName)
                .action(action)
                .payload(ObjectImportContext.of(objectName, jobId, null))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sheets(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get(ImportFileService.SHEETS);
    }

    private static List<Object> sheetStatuses(Map<String, Object> result) {
        return sheets(result).stream().map(sheet -> sheet.get(STATUS)).toList();
    }

    @SuppressWarnings("unchecked")
    private Object storedSheets(UUID jobId) {
        return ((Map<String, Object>) jobs.get(jobId).get(ImportJobMeta.FILE_INFO.getFieldName())).get(ImportJobService.SHEETS);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> sheetJobStatuses(Map<String, Object> result) {
        return ((List<Map<String, Object>>) result.get(ImportFileService.SHEET_JOBS)).stream().map(job -> job.get(STATUS)).toList();
    }

    private Map<String, Object> newJob(String objectName, ImportStatus status) {
        UUID id = UUID.randomUUID();
        Map<String, Object> job = new HashMap<>();
        job.put(ImportJobMeta.ID.getFieldName(), id);
        job.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), objectName);
        job.put(STATUS, status.name());
        job.put(ImportJobMeta.FILE_INFO.getFieldName(), new HashMap<>());
        jobs.put(id, job);
        return job;
    }

    private Map<String, Object> setStatus(UUID importId, String status) {
        Map<String, Object> job = jobs.get(runs.getOrDefault(importId, importId));
        job.put(STATUS, status);
        return new HashMap<>(job);
    }

    private void addCounters(UUID importId, long total, long errors, long success) {
        Map<String, Object> job = jobs.get(runs.getOrDefault(importId, importId));
        job.merge(TOTAL, total, (a, b) -> (Long) a + (Long) b);
        job.merge(ImportJobMeta.VALID_ROWS.getFieldName(), total - errors, (a, b) -> (Long) a + (Long) b);
        job.merge(ImportJobMeta.ERROR_ROWS.getFieldName(), errors, (a, b) -> (Long) a + (Long) b);
        job.merge(SUCCESS, success, (a, b) -> (Long) a + (Long) b);
    }
}

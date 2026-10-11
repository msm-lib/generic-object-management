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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportFileServiceTest {

    private static final String STATUS = ImportJobMeta.STATUS.getFieldName();
    private static final String VALIDATE = ObjectActionNamed.Excel.VALIDATION;
    private static final String IMPORT = ObjectActionNamed.Excel.IMPORT_DATA;

    private final Map<UUID, Map<String, Object>> jobs = new HashMap<>();
    private final List<String> executed = new ArrayList<>();
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

        when(repository.findById(eq(ImportJobMeta.OBJECT_NAME), any())).thenAnswer(inv -> jobs.get((UUID) inv.getArgument(1)));
        when(s3FileUtils.getDownloadInfo(anyMap())).thenReturn(Map.of(AttachmentInfoMeta.DOWNLOAD_URL.getFieldName(), "http://file"));
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("Order", "OrderItem"));
        when(importJobService.createSheetJob(anyMap(), anyString())).thenAnswer(inv -> newJob(inv.getArgument(1), ImportStatus.UPLOADING));
        when(actionExecutor.execute(any())).thenAnswer(inv -> {
            ActionContext<?> ctx = inv.getArgument(0);
            String step = ctx.getAction().equals(VALIDATE) || ctx.getAction().equals(ObjectActionNamed.Csv.VALIDATION) ? "validate" : "import";
            executed.add(ctx.getResource() + ":" + step);
            if (step.equals("validate")) {
                UUID jobId = (UUID) ((Map<?, ?>) ctx.getPayload()).get("importJobId");
                return setStatus(jobId, validationStatus.getOrDefault(ctx.getResource(), ImportStatus.VALIDATED.name()));
            }
            UUID jobId = ((ObjectImportContext) ctx.getPayload()).jobId();
            return setStatus(jobId, importStatus.getOrDefault(ctx.getResource(), ImportStatus.COMPLETED.name()));
        });
    }

    @Test
    void validatesThenImportsOrderSheetThenOrderItemSheet() {
        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate", "v2orderitem:import");
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.COMPLETED.name());
        assertThat(sheetJobStatuses(result)).containsExactly(ImportStatus.COMPLETED.name());
    }

    @Test
    void validationErrorsImportNothing() {
        validationStatus.put("v2order", ImportStatus.VALIDATION_FAILED.name());

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate");
        verify(importJobService, never()).createSheetJob(anyMap(), anyString());
        assertThat(result.get(STATUS)).isEqualTo(ImportStatus.VALIDATION_FAILED.name());
        assertThat(sheetJobStatuses(result)).containsExactly(ImportFileService.SKIPPED);
    }

    @Test
    void orderItemValidationErrorsLeaveItsJobFailed() {
        validationStatus.put("v2orderitem", ImportStatus.VALIDATION_FAILED.name());

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate");
        assertThat(sheetJobStatuses(result)).containsExactly(ImportStatus.VALIDATION_FAILED.name());
    }

    @Test
    void dependentSheetNameMatchesIgnoringCase() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("order", "orderitem"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import", "v2orderitem:validate", "v2orderitem:import");
        assertThat(sheetJobStatuses(result)).containsExactly(ImportStatus.COMPLETED.name());
    }

    @Test
    void missingOrderItemSheetIsSkipped() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("Order"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import");
        assertThat(sheetJobStatuses(result)).containsExactly(ImportFileService.SKIPPED);
    }

    @Test
    void singleSheetFileIsOnlyTheRootSheetEvenWhenNamedLikeADependent() {
        when(fileReaderService.getSheetNames(anyString(), anyInt())).thenReturn(List.of("OrderItem"));

        Map<String, Object> result = importExcel("v2order", newJob("v2order", ImportStatus.UPLOADING));

        assertThat(executed).containsExactly("v2order:validate", "v2order:import");
        assertThat(sheetJobStatuses(result)).containsExactly(ImportFileService.SKIPPED);
    }

    @Test
    void jobValidatedByTheOldValidateApiIsOnlyImported() {
        Map<String, Object> result = importExcel("v2orderitem", newJob("v2orderitem", ImportStatus.VALIDATED));

        assertThat(executed).containsExactly("v2orderitem:import");
        assertThat(result).doesNotContainKey(ImportFileService.SHEET_JOBS);
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
    private static List<Object> sheetJobStatuses(Map<String, Object> result) {
        return ((List<Map<String, Object>>) result.get(ImportFileService.SHEET_JOBS)).stream().map(job -> job.get(STATUS)).toList();
    }

    private Map<String, Object> newJob(String objectName, ImportStatus status) {
        UUID id = UUID.randomUUID();
        Map<String, Object> job = new HashMap<>();
        job.put(ImportJobMeta.ID.getFieldName(), id);
        job.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), objectName);
        job.put(STATUS, status.name());
        jobs.put(id, job);
        return job;
    }

    private Map<String, Object> setStatus(UUID jobId, String status) {
        jobs.get(jobId).put(STATUS, status);
        return new HashMap<>(jobs.get(jobId));
    }
}

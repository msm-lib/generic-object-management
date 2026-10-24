package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.exceptions.FieldValueInvalidException;
import com.msm.core.exceptions.FieldValueRequiredException;
import com.msm.core.exceptions.ObjectNotFoundException;
import com.msm.core.exceptions.OptimisticLockingFailureException;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.dataexchange.imports.metadata.S3FileInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
import com.msm.core.objects.dataexchange.imports.model.ImportParentContext;
import com.msm.core.objects.dataexchange.imports.model.ImportParentResult;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.exception.PayloadInvalidException;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.security.annotations.SecuredPermissionAction;
import com.msm.core.security.enums.PermissionAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.objects.dataexchange.imports.ImportDetailsTestData.DETAIL;
import static com.msm.core.objects.dataexchange.imports.ImportDetailsTestData.HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportDetailsServiceTest {

    private final UUID orderId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final ActionExecutor actionExecutor = mock(ActionExecutor.class);
    private final ObjectQueryRepository repository = mock(ObjectQueryRepository.class);
    private final ImportJobService importJobService = mock(ImportJobService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final List<String> actions = new ArrayList<>();
    private final Map<String, Object> job = new HashMap<>();
    private Map<String, Object> header;
    private String importedStatus = ImportStatus.COMPLETED.name();
    private RuntimeException checkFailure;
    private RuntimeException completeFailure;
    private ImportDetailsService service;

    @BeforeAll
    static void metadata() {
        ImportDetailsTestData.registerMetadata();
    }

    @BeforeEach
    void setUp() {
        service = new ImportDetailsService(actionExecutor, repository, ImportDetailsTestData.config(Map.of()), importJobService,
                transactionManager);
        header = new HashMap<>(Map.of("id", orderId, "code", "DH.0001", "name", "Đơn 1", "version", 4L));
        job.put(ImportJobMeta.ID.getFieldName(), jobId);
        job.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), DETAIL);
        job.put(ImportJobMeta.STATUS.getFieldName(), ImportStatus.UPLOADING.name());
        job.put(ImportJobMeta.FILE_INFO.getFieldName(), new HashMap<>(Map.of(S3FileInfoMeta.FILE_TYPE.getFieldName(), "xlsx")));
        when(repository.findById(ImportJobMeta.OBJECT_NAME, jobId)).thenAnswer(inv -> new HashMap<>(job));
        when(actionExecutor.execute(any())).thenAnswer(inv -> {
            ActionContext<?> ctx = inv.getArgument(0);
            actions.add(ctx.getAction());
            switch (ctx.getAction()) {
                case Constants.FilterAction.FILTER_OBJECT_BY_ID:
                    return header;
                case ObjectActionNamed.ImportDetails.PARENT_CHECK:
                    if (checkFailure != null) {
                        throw checkFailure;
                    }
                    return null;
                case ObjectActionNamed.Excel.IMPORT_FILE:
                    job.put(ImportJobMeta.STATUS.getFieldName(), importedStatus);
                    job.put(ImportJobMeta.SUCCESS_ROWS.getFieldName(), ImportStatus.COMPLETED.name().equals(importedStatus) ? 3L : 0L);
                    return new HashMap<>(job);
                case ObjectActionNamed.ImportDetails.PARENT_COMPLETED:
                    if (completeFailure != null) {
                        throw completeFailure;
                    }
                    return null;
                default:
                    return null;
            }
        });
    }

    @Test
    void importsIntoTheHeaderAndCompletesItOnce() {
        Map<String, Object> result = service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4)));

        assertThat(actions).containsExactly(Constants.FilterAction.FILTER_OBJECT_BY_ID, ObjectActionNamed.ImportDetails.PARENT_CHECK,
                ObjectActionNamed.Excel.IMPORT_FILE, ObjectActionNamed.ImportDetails.PARENT_COMPLETED,
                Constants.FilterAction.FILTER_OBJECT_BY_ID);
        ArgumentCaptor<ImportContext> context = ArgumentCaptor.forClass(ImportContext.class);
        verify(importJobService).saveContext(eq(jobId), context.capture());
        assertThat(context.getValue().parentObject()).isEqualTo(HEADER);
        assertThat(context.getValue().parentId()).isEqualTo(orderId);
        assertThat(context.getValue().parentCode()).isEqualTo("DH.0001");
        assertThat(context.getValue().parentVersion()).isEqualTo(4L);
        assertThat(context.getValue().mode()).isEqualTo(ImportMode.APPEND);
        assertThat(context.getValue().parentReference()).containsExactly(
                Map.entry("id", orderId), Map.entry("code", "DH.0001"), Map.entry("name", "Đơn 1"));
        assertThat(result).containsKeys(ImportDetailsService.JOB, ImportDetailsService.PARENT);
    }

    @Test
    void checkAndCompletionGetTheJobAndTheHeader() {
        List<Object> payloads = new ArrayList<>();
        doAnswer(inv -> {
            ActionContext<?> ctx = inv.getArgument(0);
            payloads.add(ctx.getPayload());
            if (ctx.getAction().equals(ObjectActionNamed.Excel.IMPORT_FILE)) {
                return Map.of(ImportJobMeta.STATUS.getFieldName(), ImportStatus.COMPLETED_WITH_ERRORS.name(),
                        ImportJobMeta.SUCCESS_ROWS.getFieldName(), 2L, ImportJobMeta.ERROR_ROWS.getFieldName(), 1L);
            }
            return ctx.getAction().equals(Constants.FilterAction.FILTER_OBJECT_BY_ID) ? header : null;
        }).when(actionExecutor).execute(any());

        service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4, "mode", "UPSERT")));

        ImportParentContext check = (ImportParentContext) payloads.get(1);
        ImportParentResult completed = (ImportParentResult) payloads.get(3);
        assertThat(check.jobId()).isEqualTo(jobId);
        assertThat(check.parent()).isEqualTo(header);
        assertThat(check.context().mode()).isEqualTo(ImportMode.UPSERT);
        assertThat(completed.successRows()).isEqualTo(2L);
        assertThat(completed.errorRows()).isEqualTo(1L);
    }

    @Test
    void headerOutsideTheUsersScopeIsNotFound() {
        header = Map.of();

        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4))))
                .isInstanceOf(ObjectNotFoundException.class);
        verify(importJobService, never()).saveContext(any(), any());
    }

    @Test
    void headerChangedByAnotherUserIsAConflict() {
        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 3))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString()))))
                .isInstanceOf(FieldValueRequiredException.class);
        assertThat(actions).doesNotContain(ObjectActionNamed.Excel.IMPORT_FILE);
    }

    @Test
    void jobMustImportDetailsOfThisHeaderAndNotBeProcessed() {
        job.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), HEADER);
        assertPayloadInvalid();

        job.put(ImportJobMeta.OBJECT_NAME_FIELD.getFieldName(), DETAIL);
        job.put(ImportJobMeta.STATUS.getFieldName(), ImportStatus.COMPLETED.name());
        assertPayloadInvalid();

        job.put(ImportJobMeta.STATUS.getFieldName(), ImportStatus.UPLOADING.name());
        job.put(ImportJobMeta.FILE_INFO.getFieldName(), Map.of(ImportJobService.CONTEXT,
                new ImportContext(HEADER, UUID.randomUUID(), "DH.0002", 1L, ImportMode.APPEND, Map.of()).toMap()));
        assertPayloadInvalid();

        job.put(ImportJobMeta.FILE_INFO.getFieldName(), Map.of(S3FileInfoMeta.FILE_TYPE.getFieldName(), "csv"));
        assertPayloadInvalid();
    }

    @Test
    void modeMustBeAllowed() {
        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4, "mode", "REPLACE"))))
                .isInstanceOf(FieldValueInvalidException.class);
        verify(importJobService, never()).saveContext(any(), any());
    }

    @Test
    void refusedByTheServiceImportsNothing() {
        checkFailure = new IllegalStateException("order is locked");

        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4))))
                .hasMessage("order is locked");
        verify(importJobService, never()).saveContext(any(), any());
        assertThat(actions).doesNotContain(ObjectActionNamed.Excel.IMPORT_FILE);
    }

    @Test
    void nothingWrittenNothingCompleted() {
        importedStatus = ImportStatus.VALIDATION_FAILED.name();

        service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4)));

        assertThat(actions).doesNotContain(ObjectActionNamed.ImportDetails.PARENT_COMPLETED);
    }

    @Test
    void failedCompletionIsReportedInTheJob() {
        completeFailure = new IllegalStateException("totals failed");

        service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(repository).update(eq(ImportJobMeta.OBJECT_NAME), eq(jobId), values.capture());
        assertThat(values.getValue().get(ImportJobMeta.STATUS.getFieldName())).isEqualTo(ImportStatus.COMPLETED_WITH_ERRORS.name());
        assertThat((String) values.getValue().get(ImportJobMeta.ERROR_MESSAGE.getFieldName()))
                .startsWith("Rows imported, but updating the testorder failed");
    }

    @Test
    void routeNeedsUpdateOnTheHeader() throws NoSuchMethodException {
        SecuredPermissionAction permission = ImportDetailsService.class.getMethod("importDetails", ActionContext.class)
                .getAnnotation(SecuredPermissionAction.class);

        assertThat(permission).isNotNull();
        assertThat(permission.anyOf()).containsExactly(PermissionAction.UPDATE);
    }

    @Test
    void importRunsOutsideTheRouteTransactionAndCompletesInItsOwn() {
        service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4)));

        ArgumentCaptor<TransactionDefinition> definitions = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager, times(2)).getTransaction(definitions.capture());
        assertThat(definitions.getAllValues()).extracting(TransactionDefinition::getPropagationBehavior)
                .containsExactly(TransactionDefinition.PROPAGATION_NOT_SUPPORTED, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private void assertPayloadInvalid() {
        assertThatThrownBy(() -> service.importDetails(request(Map.of("importJobId", jobId.toString(), "version", 4))))
                .isInstanceOf(PayloadInvalidException.class);
        verify(repository, never()).update(eq(ImportJobMeta.OBJECT_NAME), eq(jobId), anyMap());
    }

    private ActionContext<Map<String, Object>> request(Map<String, Object> payload) {
        ActionContext<Map<String, Object>> context = ActionContext.<Map<String, Object>>builder()
                .resource(HEADER)
                .action(ObjectActionNamed.ImportDetails.IMPORT_DETAILS)
                .payload(new HashMap<>(payload))
                .build();
        context.setObjectId(orderId);
        return context;
    }
}

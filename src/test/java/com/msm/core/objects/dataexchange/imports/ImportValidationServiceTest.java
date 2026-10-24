package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
import com.msm.core.objects.dataexchange.imports.model.ImportRow;
import com.msm.core.objects.entity.metadata.ImportErrorMeta;
import com.msm.core.objects.entity.metadata.ImportStagingMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import org.jooq.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.msm.core.objects.dataexchange.imports.ImportDetailsTestData.DETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportValidationServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_A = UUID.randomUUID();
    private static final UUID PRODUCT_B = UUID.randomUUID();
    private static final UUID UNIT = UUID.randomUUID();

    private final UUID importId = UUID.randomUUID();
    private final ObjectQueryRepository repository = mock(ObjectQueryRepository.class);
    private final ImportJobService importJobService = mock(ImportJobService.class);
    private final com.msm.core.objects.dataexchange.imports.validation.ImportValidationService rowValidation =
            mock(com.msm.core.objects.dataexchange.imports.validation.ImportValidationService.class);
    private ImportValidationService service;

    @BeforeAll
    static void metadata() {
        ImportDetailsTestData.registerMetadata();
    }

    @BeforeEach
    void setUp() {
        service = new ImportValidationService(rowValidation, mock(ActionExecutor.class), repository,
                ImportDetailsTestData.config(Map.of(
                        "imports.objects.testorderitem.identity.fields[0]", "orderId",
                        "imports.objects.testorderitem.identity.fields[1]", "productId")), importJobService);
        when(importJobService.jobIdOf(any())).thenAnswer(inv -> inv.getArgument(0));
        // nothing staged yet, the order has product A
        when(repository.findByCondition(eq(ImportStagingMeta.OBJECT_NAME), any(Condition.class), anyList())).thenReturn(List.of());
        when(repository.findByCondition(eq(DETAIL), any(Condition.class), anyList()))
                .thenReturn(List.of(Map.of("orderId", ORDER_ID, "productId", PRODUCT_A, "unitId", UNIT)));
    }

    @Test
    void appendRefusesRowsTheHeaderAlreadyHasAndRowsRepeatedInTheFile() {
        context(ImportMode.APPEND);

        service.processValidate(importId, ObjectMetadataFactory.getObjectMetadataByName(DETAIL), List.of(
                row(1, PRODUCT_A), row(2, PRODUCT_B), row(3, PRODUCT_B)));

        assertThat(errorCodes()).containsExactly(
                Map.entry(1L, ImportValidationService.DUPLICATE_IN_PARENT),
                Map.entry(3L, ImportValidationService.DUPLICATE_IN_FILE));
    }

    @Test
    void upsertAcceptsRowsTheHeaderAlreadyHas() {
        context(ImportMode.UPSERT);

        service.processValidate(importId, ObjectMetadataFactory.getObjectMetadataByName(DETAIL), List.of(row(1, PRODUCT_A), row(2, PRODUCT_B)));

        verify(repository, never()).saveAll(eq(ImportErrorMeta.OBJECT_NAME), anyList());
    }

    @Test
    void importWithoutHeaderChecksNoDuplicates() {
        service.processValidate(importId, ObjectMetadataFactory.getObjectMetadataByName(DETAIL), List.of(row(1, PRODUCT_A), row(2, PRODUCT_A)));

        verify(repository, never()).saveAll(eq(ImportErrorMeta.OBJECT_NAME), anyList());
    }

    private void context(ImportMode mode) {
        when(importJobService.contextOf(importId)).thenReturn(new ImportContext("testorder", ORDER_ID, "DH.0001", 1L, mode, Map.of()));
    }

    private static ImportRow row(long rowNumber, UUID productId) {
        return ImportRow.of(rowNumber, new HashMap<>(Map.of("orderId", ORDER_ID, "productId", productId, "unitId", UNIT)));
    }

    @SuppressWarnings("unchecked")
    private List<Map.Entry<Long, Object>> errorCodes() {
        ArgumentCaptor<List<Map<String, Object>>> errors = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(eq(ImportErrorMeta.OBJECT_NAME), errors.capture());
        return errors.getValue().stream()
                .map(error -> Map.entry(((Number) error.get(ImportErrorMeta.ROW_NUMBER.getFieldName())).longValue(),
                        error.get(ImportErrorMeta.ERROR_CODE.getFieldName())))
                .toList();
    }
}

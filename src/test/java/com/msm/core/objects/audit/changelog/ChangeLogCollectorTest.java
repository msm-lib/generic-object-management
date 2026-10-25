package com.msm.core.objects.audit.changelog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChangeLogCollectorTest {

    private final ChangeLogOutboxStore store = mock(ChangeLogOutboxStore.class);
    private final ChangeLogCollector collector = new ChangeLogCollector(store);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private ChangeLogEntry entry() {
        return new ChangeLogEntry(UUID.randomUUID(), Instant.now(), "bhc", "ORDER_SERVICE", "tx", "CREATE", "testorder",
                "1", null, null, null, List.of(), null);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @Test
    void withoutTransactionTheEntriesAreInsertedAtOnce() {
        ChangeLogEntry entry = entry();

        collector.add(List.of(entry));

        verify(store).insert(List.of(entry));
    }

    @Test
    void inATransactionTheEntriesWaitForBeforeCommit() {
        beginTransaction();
        ChangeLogEntry first = entry();
        ChangeLogEntry second = entry();

        collector.add(List.of(first));
        collector.add(List.of(second));
        verify(store, never()).insert(any());

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(synchronization -> synchronization.beforeCommit(false));
        verify(store).insert(List.of(first, second));
    }

    @Test
    void txIdIsSharedInsideATransaction() {
        beginTransaction();

        assertThat(collector.txId()).isEqualTo(collector.txId());
    }
}

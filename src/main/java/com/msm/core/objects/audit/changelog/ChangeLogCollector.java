package com.msm.core.objects.audit.changelog;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Keeps the entries of the running transaction and writes them to the outbox in {@code beforeCommit}: still inside the
 * transaction, so a row and its change log are committed or rolled back together, and a failing outbox insert fails the
 * write. Without a transaction (a route running {@code NOT_SUPPORTED}, a job) every write auto-commits, so its entries
 * are inserted at once.
 *
 * <p>The buffer lives in the synchronization, not in a resource bound to the thread: Spring suspends the synchronizations
 * of the outer transaction around a {@code REQUIRES_NEW} one, so each transaction has its own buffer.
 */
@RequiredArgsConstructor
public class ChangeLogCollector {

    private final ChangeLogOutboxStore store;

    public void add(Collection<ChangeLogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        Buffer buffer = currentBuffer();
        if (buffer == null) {
            store.insert(entries);
            return;
        }
        buffer.entries.addAll(entries);
    }

    /** Id shared by the entries of the running transaction when the request has no trace id. */
    public String txId() {
        Buffer buffer = currentBuffer();
        return buffer != null ? buffer.txId : UUID.randomUUID().toString();
    }

    private Buffer currentBuffer() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return null;
        }
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof Buffer buffer && buffer.owner == this) {
                return buffer;
            }
        }
        Buffer buffer = new Buffer(this);
        TransactionSynchronizationManager.registerSynchronization(buffer);
        return buffer;
    }

    private static final class Buffer implements TransactionSynchronization {
        private final ChangeLogCollector owner;
        private final String txId = UUID.randomUUID().toString();
        private final List<ChangeLogEntry> entries = new ArrayList<>();

        private Buffer(ChangeLogCollector owner) {
            this.owner = owner;
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            if (!entries.isEmpty()) {
                owner.store.insert(new ArrayList<>(entries));
                entries.clear();
            }
        }

        @Override
        public void afterCompletion(int status) {
            entries.clear();
        }
    }
}

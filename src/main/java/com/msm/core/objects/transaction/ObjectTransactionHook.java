package com.msm.core.objects.transaction;

import com.msm.core.hook.common.TransactionHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
//@Component
public class ObjectTransactionHook implements TransactionHook {

    @Override
    public void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                // optional: log rollback if we need
                                log.warn("Transaction rolled back, skip AFTER_COMMIT hook");
                            }
                        }
                    });
        } else {
            task.run();
        }
    }
}
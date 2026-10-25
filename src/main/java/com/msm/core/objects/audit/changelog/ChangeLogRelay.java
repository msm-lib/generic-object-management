package com.msm.core.objects.audit.changelog;

import com.msm.core.objects.config.GenericObjectConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends the outbox entries to {@code POST /notification/api/v1/internal/audit-logs/batch}, one batch per tenant, on its
 * own single thread (the service does not need {@code @EnableScheduling}). Several instances can run it: rows are
 * claimed with {@code FOR UPDATE SKIP LOCKED}. A batch the notification service rejects (4xx) is sent again entry by
 * entry, so one bad entry does not hold back the others; a failing entry is retried with a growing delay, then stays
 * FAILED after {@code max-attempts}.
 */
@Slf4j
public class ChangeLogRelay implements SmartLifecycle {

    static final String PATH = "/notification/api/v1/internal/audit-logs/batch";
    private static final String TENANT_HEADER = "x-tenant";
    private static final Duration MAX_DELAY = Duration.ofHours(1);
    private static final int MAX_ERROR_LENGTH = 2000;

    private final ChangeLogOutboxStore store;
    private final ChangeLogProperties properties;
    private final GenericObjectConfigProperties objectProperties;
    private final RestClient restClient;
    private ScheduledExecutorService executor;

    public ChangeLogRelay(ChangeLogOutboxStore store, ChangeLogProperties properties,
                          GenericObjectConfigProperties objectProperties, RestClient restClient) {
        this.store = store;
        this.properties = properties;
        this.objectProperties = objectProperties;
        this.restClient = restClient;
    }

    /** One round: claim, send, mark. Returns the number of entries claimed. */
    public int relayOnce() {
        ChangeLogProperties.Relay relay = properties.getRelay();
        List<ChangeLogOutboxStore.OutboxRow> rows = new ArrayList<>(store.claim(relay.getBatchSize(), Instant.now().plus(relay.getLockTimeout())));
        // UPDATE … RETURNING keeps no order
        rows.sort(Comparator.comparing(ChangeLogOutboxStore.OutboxRow::createdAt));
        Map<String, List<ChangeLogOutboxStore.OutboxRow>> byTenant = new LinkedHashMap<>();
        for (ChangeLogOutboxStore.OutboxRow row : rows) {
            byTenant.computeIfAbsent(Objects.toString(row.tenantCode(), ""), key -> new ArrayList<>()).add(row);
        }
        byTenant.forEach(this::sendTenant);
        return rows.size();
    }

    private void sendTenant(String tenant, List<ChangeLogOutboxStore.OutboxRow> rows) {
        try {
            post(tenant, rows);
            store.markSent(rows.stream().map(ChangeLogOutboxStore.OutboxRow::id).toList());
        } catch (HttpClientErrorException e) {
            if (rows.size() == 1) {
                fail(rows.get(0), e);
                return;
            }
            for (ChangeLogOutboxStore.OutboxRow row : rows) {
                sendOne(tenant, row);
            }
        } catch (RuntimeException e) {
            rows.forEach(row -> fail(row, e));
        }
    }

    private void sendOne(String tenant, ChangeLogOutboxStore.OutboxRow row) {
        try {
            post(tenant, List.of(row));
            store.markSent(List.of(row.id()));
        } catch (RuntimeException e) {
            fail(row, e);
        }
    }

    private void post(String tenant, List<ChangeLogOutboxStore.OutboxRow> rows) {
        RestClient.RequestBodySpec request = restClient.post()
                .uri(notificationUrl() + PATH)
                .contentType(MediaType.APPLICATION_JSON);
        if (!tenant.isBlank()) {
            request = request.header(TENANT_HEADER, tenant);
        }
        request.body(rows.stream().map(ChangeLogOutboxStore.OutboxRow::auditLog).toList())
                .retrieve()
                .toBodilessEntity();
    }

    private void fail(ChangeLogOutboxStore.OutboxRow row, RuntimeException e) {
        String error = e instanceof HttpClientErrorException http
                ? http.getStatusCode() + " " + http.getResponseBodyAsString()
                : e.getClass().getSimpleName() + ": " + e.getMessage();
        if (error.length() > MAX_ERROR_LENGTH) {
            error = error.substring(0, MAX_ERROR_LENGTH);
        }
        Instant next = row.attempts() >= properties.getRelay().getMaxAttempts() ? null : Instant.now().plus(delay(row.attempts()));
        log.warn("Change log {} of {} {} not sent (attempt {}): {}", row.id(), row.entityType(), row.entityId(), row.attempts(), error);
        store.markFailed(row.id(), error, next);
    }

    /** interval × 2^(attempts − 1), at most one hour. */
    Duration delay(int attempts) {
        Duration delay = properties.getRelay().getInterval();
        for (int i = 1; i < attempts && delay.compareTo(MAX_DELAY) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    /** {@code relay.url}, else the internal host of the notification service, else the gateway; without a trailing slash. */
    String notificationUrl() {
        String url = properties.getRelay().getUrl();
        if (url == null || url.isBlank()) {
            url = objectProperties.getApiConfig().getServiceUrls().get("notification");
        }
        if (url == null || url.isBlank()) {
            url = objectProperties.getApiConfig().getBaseUrl();
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("No notification URL: set objects.change-log.relay.url or objects.api-config.service-urls.notification");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void runSafely() {
        try {
            int claimed;
            do {
                claimed = relayOnce();
            } while (claimed >= properties.getRelay().getBatchSize());
            store.deleteSentBefore(Instant.now().minus(properties.getRelay().getRetention()));
        } catch (RuntimeException e) {
            log.error("Change log relay failed", e);
        }
    }

    @Override
    public synchronized void start() {
        if (executor != null || !properties.getRelay().isEnabled()) {
            return;
        }
        long interval = properties.getRelay().getInterval().toMillis();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "change-log-relay");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::runSafely, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return executor != null;
    }
}

package com.msm.core.objects.audit.changelog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.dynamicquery.ObjectQuery;
import com.msm.core.dynamicquery.command.DefaultDynamicDelete;
import com.msm.core.dynamicquery.command.DefaultDynamicInsert;
import com.msm.core.dynamicquery.command.DefaultDynamicUpdate;
import com.msm.core.dynamicquery.internal.InternalFilterQuery;
import com.msm.core.dynamicquery.internal.InternalQueryService;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.audit.DefaultAuditStrategy;
import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.converter.DefaultCustomValueMappingStrategy;
import com.msm.core.objects.repository.InternalObjectQueryRepository;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.objects.service.GenericObjectMetadataService;
import com.sun.net.httpserver.HttpServer;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The change log against a real Postgres: repository, outbox, transactions and relay.
 * {@code CHANGELOG_PG_URL=jdbc:postgresql://localhost:55432/changelog ./gradlew test --tests '*ChangeLogPostgresIT'}
 * (user {@code CHANGELOG_PG_USER}, default postgres; password {@code CHANGELOG_PG_PASSWORD}, default test).
 */
@EnabledIfEnvironmentVariable(named = "CHANGELOG_PG_URL", matches = ".+")
class ChangeLogPostgresIT {

    private static final String ORDER = "testorder";
    private static final String ITEM = "testitem";
    private static final String OTHER = "testother";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Same statements as the README migration. */
    private static final String OUTBOX_DDL = """
            create table if not exists audit_log_outbox (
                id               uuid primary key,
                created_at       timestamptz  not null,
                tenant_code      varchar(60),
                source_service   varchar(60),
                tx_id            varchar(100),
                action           varchar(20)  not null,
                entity_type      varchar(100) not null,
                entity_id        varchar(100),
                entity_parent_id varchar(100),
                entity_code      varchar(255),
                actor            jsonb,
                changed_fields   jsonb,
                metadata         jsonb,
                status           varchar(20)  not null default 'PENDING',
                attempts         integer      not null default 0,
                next_attempt_at  timestamptz  not null default now(),
                locked_until     timestamptz,
                sent_at          timestamptz,
                last_error       text
            );
            create index if not exists idx_audit_log_outbox_status on audit_log_outbox (status, next_attempt_at);
            create index if not exists idx_audit_log_outbox_entity on audit_log_outbox (entity_type, entity_id);
            """;

    private static DataSource dataSource;
    private static DSLContext dsl;
    private static TransactionTemplate tx;
    private static TransactionTemplate requiresNew;

    private ChangeLogOutboxStore store;
    private ChangeLogProperties properties;
    private ObjectQueryRepository repository;
    private HttpServer server;

    @BeforeAll
    static void connect() {
        DriverManagerDataSource driver = new DriverManagerDataSource(System.getenv("CHANGELOG_PG_URL"),
                Optional.ofNullable(System.getenv("CHANGELOG_PG_USER")).orElse("postgres"),
                Optional.ofNullable(System.getenv("CHANGELOG_PG_PASSWORD")).orElse("test"));
        dataSource = driver;
        // as Spring Boot's jOOQ auto-configuration: statements join the Spring transaction of the thread
        dsl = DSL.using(new TransactionAwareDataSourceProxy(driver), SQLDialect.POSTGRES);
        DataSourceTransactionManager manager = new DataSourceTransactionManager(driver);
        tx = new TransactionTemplate(manager);
        requiresNew = new TransactionTemplate(manager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @BeforeEach
    void setUp() {
        dsl.execute("drop table if exists test_order, test_item_hard, audit_log_outbox");
        dsl.execute("""
                create table test_order (
                    id uuid primary key default gen_random_uuid(),
                    code varchar(60) constraint uk_test_order_code unique,
                    note varchar(4000),
                    customer_id uuid,
                    quantity numeric(18, 3),
                    is_deleted boolean not null default false,
                    version bigint not null default 0,
                    created_at timestamptz, created_by varchar(100), created_by_id uuid,
                    updated_at timestamptz, updated_by varchar(100), updated_by_id uuid,
                    deleted_at timestamptz, deleted_by varchar(100), deleted_by_id uuid)""");
        dsl.execute("create table test_item_hard (id uuid primary key default gen_random_uuid(), code varchar(60), note varchar(200))");
        dsl.execute(OUTBOX_DDL);

        ObjectMetadata order = ObjectMetadata.builder().name(ORDER).tableName("test_order").attributes(List.of(
                attribute("id", "id", "UUID"), attribute("code", "code", "String"), attribute("note", "note", "String"),
                attribute("customerId", "customer_id", "UUID"), attribute("quantity", "quantity", "BigDecimal"),
                attribute("isDeleted", "is_deleted", "Boolean"), attribute("version", "version", "Long"),
                attribute("createdAt", "created_at", "Instant"), attribute("createdBy", "created_by", "String"),
                attribute("createdById", "created_by_id", "UUID"), attribute("updatedAt", "updated_at", "Instant"),
                attribute("updatedBy", "updated_by", "String"), attribute("updatedById", "updated_by_id", "UUID"),
                attribute("deletedAt", "deleted_at", "Instant"), attribute("deletedBy", "deleted_by", "String"),
                attribute("deletedById", "deleted_by_id", "UUID"))).build();
        List<Attribute> itemAttributes = List.of(attribute("id", "id", "UUID"), attribute("code", "code", "String"),
                attribute("note", "note", "String"));
        ObjectMetadata item = ObjectMetadata.builder().name(ITEM).tableName("test_item_hard").attributes(itemAttributes).build();
        ObjectMetadata other = ObjectMetadata.builder().name(OTHER).tableName("test_item_hard").attributes(itemAttributes).build();
        Map<String, ObjectMetadata> metadata = Map.of(ORDER, order, ITEM, item, OTHER, other);
        metadata.values().forEach(ObjectMetadataFactory::registerObjectMetadata);
        GenericObjectMetadataService metadataService = mock(GenericObjectMetadataService.class);
        when(metadataService.getObjectMetadata(anyString())).thenAnswer(call -> Optional.ofNullable(metadata.get(call.<String>getArgument(0))));

        properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "objects.change-log.enabled", "true",
                "objects.change-log.source-service", "ORDER_SERVICE",
                "objects.change-log.default-tenant", "bhc",
                "objects.change-log.objects[0]", ORDER,
                "objects.change-log.objects[1]", ITEM,
                "objects.change-log.object-options." + ORDER + ".parent-field", "customerId")))
                .bindOrCreate("objects.change-log", ChangeLogProperties.class);
        store = new ChangeLogOutboxStore(dsl);
        DefaultChangeLogRecorder recorder = new DefaultChangeLogRecorder(properties, new ChangeLogCollector(store), new ChangeLogDiff());
        DefaultDynamicUpdate update = new DefaultDynamicUpdate(dsl);
        ObjectQuery query = new InternalQueryService(new InternalFilterQuery(dsl), new DefaultDynamicInsert(dsl), update,
                new DefaultDynamicDelete(dsl, update));
        repository = new InternalObjectQueryRepository(query, metadataService, input -> new DefaultAuditStrategy(),
                input -> new DefaultCustomValueMappingStrategy(), recorder, new UniqueConstraintColumns(dsl));
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createThenUpdateInTransactions() {
        UUID customerId = UUID.randomUUID();
        Map<String, Object> saved = tx.execute(status -> repository.save(ORDER,
                row("code", "DH1", "note", "a", "quantity", new BigDecimal("10"), "customerId", customerId)));
        UUID id = (UUID) saved.get("id");
        tx.executeWithoutResult(status -> repository.update(ORDER, id, row("note", "b", "quantity", 10, "version", saved.get("version"))));

        List<Map<String, Object>> outbox = outbox();
        assertThat(outbox).extracting(entry -> entry.get("action")).containsExactly("CREATE", "UPDATE");
        Map<String, Object> created = outbox.get(0);
        assertThat(created).containsEntry("entity_type", ORDER).containsEntry("entity_id", id.toString())
                .containsEntry("entity_code", "DH1").containsEntry("entity_parent_id", customerId.toString())
                .containsEntry("tenant_code", "bhc").containsEntry("source_service", "ORDER_SERVICE").containsEntry("status", "PENDING");
        assertThat(fields(created)).extracting(field -> field.get("name")).contains("id", "code", "note", "quantity", "isDeleted")
                .doesNotContain("version", "createdAt", "createdBy");
        assertThat(fields(outbox.get(1))).containsExactly(Map.of("name", "note", "before", "a", "after", "b", "dataType", "text"));
        assertThat(json(outbox.get(1).get("actor"))).containsEntry("type", "SYSTEM");
        assertThat(outbox.get(0).get("tx_id")).isNotEqualTo(outbox.get(1).get("tx_id"));
    }

    @Test
    void rolledBackWriteLeavesNoLog() {
        tx.executeWithoutResult(status -> {
            repository.save(ORDER, row("code", "RB1"));
            status.setRollbackOnly();
        });

        assertThat(count("test_order")).isZero();
        assertThat(outbox()).isEmpty();
    }

    @Test
    void failingOutboxFailsTheWrite() {
        dsl.execute("drop table audit_log_outbox");

        assertThatThrownBy(() -> tx.execute(status -> repository.save(ORDER, row("code", "FAIL1"))))
                .hasMessageContaining("audit_log_outbox");
        assertThat(count("test_order")).isZero();
    }

    @Test
    void requiresNewTransactionKeepsItsOwnLog() {
        tx.executeWithoutResult(status -> {
            repository.save(ORDER, row("code", "OUTER"));
            requiresNew.executeWithoutResult(inner -> repository.save(ORDER, row("code", "INNER")));
            status.setRollbackOnly();
        });

        assertThat(dsl.fetch("select code from test_order").getValues(0, String.class)).containsExactly("INNER");
        assertThat(outbox()).extracting(entry -> entry.get("entity_code")).containsExactly("INNER");
    }

    @Test
    void withoutTransactionTheLogIsWrittenAtOnce() {
        repository.save(ORDER, row("code", "AUTO1"));

        assertThat(outbox()).extracting(entry -> entry.get("action")).containsExactly("CREATE");
    }

    @Test
    void updateByConditionLogsEveryRowItTouched() {
        repository.save(ORDER, row("code", "C1", "note", "a"));
        repository.save(ORDER, row("code", "C2", "note", "b"));
        repository.save(ORDER, row("code", "C3", "note", "c"));
        clearOutbox();

        int rows = tx.execute(status -> repository.updateWithCondition(ORDER, DSL.field(DSL.name("code")).in("C1", "C2"), row("note", "x")));

        assertThat(rows).isEqualTo(2);
        List<Map<String, Object>> outbox = outbox();
        assertThat(outbox).extracting(entry -> entry.get("entity_code")).containsExactlyInAnyOrder("C1", "C2");
        assertThat(outbox).allSatisfy(entry -> assertThat(fields(entry)).extracting(field -> field.get("after")).containsExactly("x"));
    }

    @Test
    void upsertOnFieldsAndOnConstraintTellsCreateFromUpdate() {
        repository.save(ORDER, row("code", "U1", "note", "a"));
        clearOutbox();

        tx.executeWithoutResult(status -> {
            repository.upsert(ORDER, row("code", "U1", "note", "b"), List.of("code"), null);
            repository.upsert(ORDER, row("code", "U2", "note", "n"), List.of("code"), null);
        });
        List<Map<String, Object>> first = outbox();
        assertThat(first).extracting(entry -> entry.get("entity_code") + ":" + entry.get("action"))
                .containsExactlyInAnyOrder("U1:UPDATE", "U2:CREATE");
        assertThat(fields(byCode(first, "U1"))).containsExactly(Map.of("name", "note", "before", "a", "after", "b", "dataType", "text"));
        clearOutbox();

        tx.executeWithoutResult(status -> repository.bulkUpsertReturning(ORDER,
                new ArrayList<>(List.of(row("code", "U1", "note", "c"), row("code", "U3", "note", "m"))), "uk_test_order_code"));
        List<Map<String, Object>> second = outbox();
        assertThat(second).extracting(entry -> entry.get("entity_code") + ":" + entry.get("action"))
                .containsExactlyInAnyOrder("U1:UPDATE", "U3:CREATE");
        assertThat(fields(byCode(second, "U1"))).containsExactly(Map.of("name", "note", "before", "b", "after", "c", "dataType", "text"));
    }

    @Test
    void upsertOnAnUnknownConstraintIsRefusedThenWorksOnceTheConstraintExists() {
        dsl.execute("create unique index uk_test_order_note_idx on test_order (note)");
        List<Map<String, Object>> rows = new ArrayList<>(List.of(row("code", "K1", "note", "n1")));

        assertThatThrownBy(() -> tx.execute(status -> repository.bulkUpsertReturning(ORDER, rows, "uk_test_order_note")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uk_test_order_note");
        assertThat(count("test_order")).isZero();
        assertThat(outbox()).isEmpty();

        // the empty answer was not cached: a constraint created afterwards is found on the next call
        dsl.execute("alter table test_order add constraint uk_test_order_note unique using index uk_test_order_note_idx");
        tx.execute(status -> repository.bulkUpsertReturning(ORDER, new ArrayList<>(List.of(row("code", "K1", "note", "n1"))),
                "uk_test_order_note"));

        assertThat(outbox()).extracting(entry -> entry.get("entity_code") + ":" + entry.get("action")).containsExactly("K1:CREATE");
    }

    @Test
    void deleteByIdsLogsExactlyTheRowsDeleted() {
        Map<String, Object> a = repository.save(ORDER, row("code", "D1"));
        Map<String, Object> b = repository.save(ORDER, row("code", "D2"));
        repository.save(ORDER, row("code", "D3"));
        clearOutbox();

        tx.executeWithoutResult(status -> repository.delete(ORDER, new ArrayList<>(List.of(
                row("id", a.get("id"), "version", a.get("version")), row("id", b.get("id"), "version", b.get("version"))))));

        Set<String> deletedInDb = new HashSet<>(dsl.fetch("select id::text from test_order where is_deleted").getValues(0, String.class));
        assertThat(deletedInDb).hasSize(2);
        List<Map<String, Object>> outbox = outbox();
        assertThat(outbox).extracting(entry -> (String) entry.get("entity_id")).containsExactlyInAnyOrderElementsOf(deletedInDb);
        assertThat(outbox).allSatisfy(entry -> {
            assertThat(entry.get("action")).isEqualTo("DELETE");
            assertThat(json(entry.get("metadata"))).containsEntry("deleteType", "SOFT");
        });
    }

    @Test
    void deleteWithAWrongVersionFailsAndLogsNothing() {
        Map<String, Object> a = repository.save(ORDER, row("code", "V1"));
        Map<String, Object> b = repository.save(ORDER, row("code", "V2"));
        clearOutbox();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.delete(ORDER, new ArrayList<>(List.of(
                row("id", a.get("id"), "version", a.get("version")), row("id", b.get("id"), "version", 999L))))))
                .hasMessageContaining("Version conflict");

        assertThat(value("select count(*) from test_order where is_deleted", Integer.class)).isZero();
        assertThat(outbox()).isEmpty();
    }

    @Test
    void softDeleteByIdAndHardDelete() {
        Map<String, Object> order = repository.save(ORDER, row("code", "S1", "note", "keep me"));
        Map<String, Object> item = repository.save(ITEM, row("code", "H1"));
        clearOutbox();

        tx.executeWithoutResult(status -> {
            repository.delete(ORDER, order.get("id"), (Long) order.get("version"));
            repository.delete(ITEM, item.get("id"), new HashMap<>());
        });

        assertThat(count("test_item_hard")).isZero();
        List<Map<String, Object>> outbox = outbox();
        assertThat(outbox).extracting(entry -> entry.get("entity_code") + ":" + json(entry.get("metadata")).get("deleteType"))
                .containsExactlyInAnyOrder("S1:SOFT", "H1:HARD");
        assertThat(fields(byCode(outbox, "S1"))).filteredOn(field -> "note".equals(field.get("name")))
                .singleElement().satisfies(field -> assertThat(field).containsEntry("before", "keep me").containsEntry("after", null));
    }

    @Test
    void insertBatchIgnoringDuplicatesLogsOnlyTheRowsInserted() {
        repository.save(ORDER, row("code", "B1"));
        clearOutbox();

        tx.executeWithoutResult(status -> repository.insertBatchIgnoreDuplicate(ORDER,
                new ArrayList<>(List.of(row("code", "B1", "note", "dup"), row("code", "B2", "note", "new")))));

        List<Map<String, Object>> outbox = outbox();
        assertThat(outbox).extracting(entry -> entry.get("entity_code")).containsExactly("B2");
        String idInDb = value("select id::text from test_order where code = 'B2'", String.class);
        assertThat(outbox.get(0)).containsEntry("entity_id", idInDb);
    }

    @Test
    void objectWithoutChangeLogWritesNoLog() {
        tx.execute(status -> repository.save(OTHER, row("code", "O1")));

        assertThat(count("test_item_hard")).isEqualTo(1);
        assertThat(outbox()).isEmpty();
    }

    @Test
    void claimSkipsRowsAnotherInstanceHolds() throws Exception {
        for (int i = 0; i < 4; i++) {
            repository.save(ORDER, row("code", "L" + i));
        }
        try (Connection other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            Set<UUID> held = new HashSet<>();
            try (Statement statement = other.createStatement();
                 ResultSet rs = statement.executeQuery("select id from audit_log_outbox order by created_at limit 2 for update")) {
                while (rs.next()) {
                    held.add(rs.getObject(1, UUID.class));
                }
            }

            List<ChangeLogOutboxStore.OutboxRow> claimed = store.claim(10, Instant.now().plusSeconds(300));

            assertThat(claimed).hasSize(2).extracting(ChangeLogOutboxStore.OutboxRow::id).doesNotContainAnyElementsOf(held);
            assertThat(claimed).allSatisfy(row -> assertThat(row.attempts()).isEqualTo(1));
            other.rollback();
        }
        assertThat(value("select count(*) from audit_log_outbox where status = 'SENDING'", Integer.class)).isEqualTo(2);
    }

    @Test
    void relaySendsMarksSentRetriesAndCleansUp() throws IOException {
        List<String> bodies = new CopyOnWriteArrayList<>();
        List<String> tenants = new CopyOnWriteArrayList<>();
        AtomicInteger status = new AtomicInteger(200);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tenants.add(exchange.getRequestHeaders().getFirst("x-tenant"));
            byte[] response = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        properties.getRelay().setUrl("http://localhost:" + server.getAddress().getPort());
        ChangeLogRelay relay = new ChangeLogRelay(store, properties, new GenericObjectConfigProperties(), RestClient.create());
        Map<String, Object> saved = repository.save(ORDER, row("code", "R1", "note", "a"));
        repository.update(ORDER, saved.get("id"), row("note", "b", "version", saved.get("version")));

        assertThat(relay.relayOnce()).isEqualTo(2);

        assertThat(bodies).hasSize(1);
        assertThat(tenants).containsExactly("bhc");
        List<Map<String, Object>> sent = JSON.readValue(bodies.get(0), new TypeReference<>() {});
        assertThat(sent).extracting(log -> log.get("action")).containsExactlyInAnyOrder("CREATE", "UPDATE");
        assertThat(sent).allSatisfy(log -> assertThat(log).containsEntry("logCategory", "DATA_CHANGE")
                .containsEntry("sourceService", "ORDER_SERVICE").containsEntry("entityCode", "R1").containsKey("timestamp"));
        assertThat(value("select count(*) from audit_log_outbox where status = 'SENT' and sent_at is not null", Integer.class))
                .isEqualTo(2);

        status.set(400);
        repository.save(ORDER, row("code", "R2"));
        relay.relayOnce();
        Map<String, Object> failed = dsl.fetch("select status, attempts, last_error, next_attempt_at > now() as later "
                + "from audit_log_outbox where entity_code = 'R2'").intoMaps().get(0);
        assertThat(failed).containsEntry("status", "PENDING").containsEntry("attempts", 1).containsEntry("later", true);
        assertThat((String) failed.get("last_error")).startsWith("400");
        assertThat(relay.relayOnce()).as("not due yet").isZero();

        assertThat(store.deleteSentBefore(Instant.now().plusSeconds(1))).isEqualTo(2);
        assertThat(count("audit_log_outbox")).isEqualTo(1);
    }

    private List<Map<String, Object>> outbox() {
        return dsl.fetch("select * from audit_log_outbox order by created_at, entity_code").intoMaps();
    }

    private <T> T value(String sql, Class<T> type) {
        return dsl.fetchSingle(sql).get(0, type);
    }

    private void clearOutbox() {
        dsl.execute("delete from audit_log_outbox");
    }

    private int count(String table) {
        return dsl.fetchCount(DSL.table(DSL.name(table)));
    }

    private Map<String, Object> byCode(List<Map<String, Object>> outbox, String code) {
        return outbox.stream().filter(entry -> code.equals(entry.get("entity_code"))).findFirst().orElseThrow();
    }

    private List<Map<String, Object>> fields(Map<String, Object> entry) {
        try {
            return JSON.readValue(String.valueOf(entry.get("changed_fields")), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> json(Object value) {
        try {
            return value == null ? Map.of() : JSON.readValue(String.valueOf(value), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static Attribute attribute(String fieldName, String columnName, String fieldType) {
        return Attribute.builder().fieldName(fieldName).columnName(columnName).fieldType(fieldType).build();
    }
}

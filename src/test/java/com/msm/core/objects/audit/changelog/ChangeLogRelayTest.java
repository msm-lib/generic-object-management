package com.msm.core.objects.audit.changelog;

import com.msm.core.objects.config.GenericObjectConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChangeLogRelayTest {

    private static final String URL = "http://notification.internal:8290/notification/api/v1/internal/audit-logs/batch";

    private final ChangeLogOutboxStore store = mock(ChangeLogOutboxStore.class);
    private final ChangeLogProperties properties = ChangeLogTestData.properties(Map.of());

    private ChangeLogOutboxStore.OutboxRow row(int attempts) {
        return new ChangeLogOutboxStore.OutboxRow(UUID.randomUUID(), Instant.parse("2026-10-25T03:00:00Z"), "bhc", "ORDER_SERVICE",
                "tx", "UPDATE", "v2order", "1", null, "DH1", Map.of("type", "SYSTEM"),
                List.of(Map.of("name", "note", "before", "a", "after", "b")), null, attempts);
    }

    private GenericObjectConfigProperties objectProperties(String notificationUrl) {
        GenericObjectConfigProperties objectProperties = new GenericObjectConfigProperties();
        objectProperties.getApiConfig().setBaseUrl("https://gw.example");
        if (notificationUrl != null) {
            objectProperties.getApiConfig().getServiceUrls().put("notification", notificationUrl);
        }
        return objectProperties;
    }

    @Test
    void batchIsSentWithTheTenantAndMarkedSent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChangeLogRelay relay = new ChangeLogRelay(store, properties, objectProperties("http://notification.internal:8290/"), builder.build());
        ChangeLogOutboxStore.OutboxRow first = row(1);
        ChangeLogOutboxStore.OutboxRow second = row(1);
        when(store.claim(anyInt(), any())).thenReturn(List.of(first, second));
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andExpect(header("x-tenant", "bhc"))
                .andExpect(jsonPath("$[0].logCategory").value("DATA_CHANGE"))
                .andExpect(jsonPath("$[0].changedFields[0].name").value("note"))
                .andExpect(jsonPath("$[1].entityCode").value("DH1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(relay.relayOnce()).isEqualTo(2);

        server.verify();
        verify(store).markSent(List.of(first.id(), second.id()));
    }

    @Test
    void rejectedBatchIsSentEntryByEntry() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChangeLogRelay relay = new ChangeLogRelay(store, properties, objectProperties("http://notification.internal:8290"), builder.build());
        ChangeLogOutboxStore.OutboxRow good = row(1);
        ChangeLogOutboxStore.OutboxRow bad = row(1);
        when(store.claim(anyInt(), any())).thenReturn(List.of(good, bad));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(requestTo(URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad action"));

        relay.relayOnce();

        server.verify();
        verify(store).markSent(List.of(good.id()));
        verify(store).markFailed(eq(bad.id()), any(), isNotNull());
    }

    @Test
    void entryFailingTooOftenStaysFailed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChangeLogRelay relay = new ChangeLogRelay(store, properties, objectProperties("http://notification.internal:8290"), builder.build());
        ChangeLogOutboxStore.OutboxRow row = row(10);
        when(store.claim(anyInt(), any())).thenReturn(List.of(row));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        relay.relayOnce();

        verify(store).markFailed(eq(row.id()), any(), isNull());
    }

    @Test
    void delayDoublesUpToOneHour() {
        ChangeLogRelay relay = new ChangeLogRelay(store, properties, objectProperties(null), RestClient.create());

        assertThat(relay.delay(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(relay.delay(3)).isEqualTo(Duration.ofMinutes(2));
        assertThat(relay.delay(10)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void urlFallsBackToTheGatewayThenFails() {
        assertThat(new ChangeLogRelay(store, properties, objectProperties(null), RestClient.create()).notificationUrl())
                .isEqualTo("https://gw.example");
        GenericObjectConfigProperties none = new GenericObjectConfigProperties();
        assertThatThrownBy(() -> new ChangeLogRelay(store, properties, none, RestClient.create()).notificationUrl())
                .isInstanceOf(IllegalStateException.class);
    }
}

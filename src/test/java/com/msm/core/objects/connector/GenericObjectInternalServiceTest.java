package com.msm.core.objects.connector;

import com.msm.core.objects.config.GenericObjectConfigProperties;
import com.msm.core.objects.integration.RequestClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GenericObjectInternalServiceTest {

    /** Binds the properties as Spring binds application.yml, so the "master-data" map key is checked as written. */
    private static GenericObjectInternalService service(Map<String, String> props) {
        GenericObjectConfigProperties properties = new Binder(new MapConfigurationPropertySource(props))
                .bindOrCreate("objects", GenericObjectConfigProperties.class);
        return new GenericObjectInternalService(properties, mock(RequestClient.class));
    }

    @Test
    void serviceHostGetsTheServicePath() {
        GenericObjectInternalService service = service(Map.of(
                "objects.api-config.base-url", "https://gw.example",
                "objects.api-config.service-urls.user", "http://nlb.internal:8281",
                "objects.api-config.service-urls.master-data", "http://nlb.internal:8287"));

        assertThat(service.getBaseUrl("organization")).isEqualTo("http://nlb.internal:8281/user");
        assertThat(service.getBaseUrl("geographylocation")).isEqualTo("http://nlb.internal:8287/master-data");
    }

    @Test
    void serviceWithoutEntryFallsBackToBaseUrl() {
        GenericObjectInternalService service = service(Map.of(
                "objects.api-config.base-url", "https://gw.example/",
                "objects.api-config.service-urls.user", "http://nlb.internal:8281"));

        assertThat(service.getBaseUrl("v2order")).isEqualTo("https://gw.example/order");
    }

    @Test
    void blankEntryFallsBackToBaseUrl() {
        // ${USER_SERVICE_URL:} with the variable not set
        GenericObjectInternalService service = service(Map.of(
                "objects.api-config.base-url", "https://gw.example",
                "objects.api-config.service-urls.user", ""));

        assertThat(service.getBaseUrl("organization")).isEqualTo("https://gw.example/user");
    }

    @Test
    void trailingSlashOfTheServiceHostIsDropped() {
        GenericObjectInternalService service = service(Map.of(
                "objects.api-config.service-urls.user", "http://nlb.internal:8281/"));

        assertThat(service.getBaseUrl("organization")).isEqualTo("http://nlb.internal:8281/user");
    }

    @Test
    void localhostShortcutAppliesToBaseUrlOnly() {
        GenericObjectInternalService service = service(Map.of(
                "objects.api-config.base-url", "http://localhost:8087",
                "objects.api-config.service-urls.order", "http://localhost:9999"));

        assertThat(service.getBaseUrl("geographylocation")).isEqualTo("http://localhost:8090/master-data");
        assertThat(service.getBaseUrl("v2order")).isEqualTo("http://localhost:9999/order");
    }

    @Test
    void objectWithoutServiceFails() {
        GenericObjectInternalService service = service(Map.of("objects.api-config.base-url", "https://gw.example"));

        assertThatThrownBy(() -> service.getBaseUrl("no-such-object"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-object");
    }
}

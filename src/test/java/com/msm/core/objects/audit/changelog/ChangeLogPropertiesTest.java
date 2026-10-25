package com.msm.core.objects.audit.changelog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeLogPropertiesTest {

    private ChangeLogProperties bind(String yaml) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
        return new Binder(ConfigurationPropertySources.from(sources))
                .bindOrCreate("objects.change-log", ChangeLogProperties.class);
    }

    @Test
    void objectsAreTurnedOnAsWrittenInApplicationYml() throws IOException {
        ChangeLogProperties properties = bind("""
                objects:
                  change-log:
                    enabled: true
                    objects: [v2order, V2OrderItem]
                    object-options:
                      v2orderitem:
                        parent-field: orderId
                      kpiallocate:
                        parent-field: headerId
                    relay:
                      retention: 7d
                """);

        assertThat(properties.objectConfig("v2order")).isNotNull();
        assertThat(properties.objectConfig("v2order").getCodeField()).isEqualTo("code");
        assertThat(properties.objectConfig("V2OrderItem").getParentField()).isEqualTo("orderId");
        assertThat(properties.objectConfig("kpiallocate")).isNull();
        assertThat(properties.objectConfig("v2shipment")).isNull();
        assertThat(properties.getRelay().getRetention()).isEqualTo(Duration.ofDays(7));
    }
}

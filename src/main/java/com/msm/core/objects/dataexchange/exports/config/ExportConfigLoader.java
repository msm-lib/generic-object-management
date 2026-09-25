package com.msm.core.objects.dataexchange.exports.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Slf4j
public class ExportConfigLoader implements EnvironmentPostProcessor {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:exports/*.yml");
            for (Resource resource : resources) {
//                if (resource.exists()) {
//                    java.util.List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
//                    for (PropertySource<?> source : propertySources) {
//                        environment.getPropertySources().addLast(source);
//                    }
//                }

                if (resource.exists()) {
                    String sourceName = "export-" + resource.getFilename();
                    java.util.List<PropertySource<?>> propertySources = loader.load(sourceName, resource);
                    for (PropertySource<?> source : propertySources) {
                        environment.getPropertySources().addFirst(source);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error while loading config import object from exports/", e);
        }
    }
}

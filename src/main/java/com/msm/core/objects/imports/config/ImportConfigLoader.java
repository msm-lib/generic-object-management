package com.msm.core.objects.imports.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

public class ImportConfigLoader implements EnvironmentPostProcessor {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:imports/*.yml");
            for (Resource resource : resources) {
                if (resource.exists()) {
                    java.util.List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
                    for (PropertySource<?> source : propertySources) {
                        environment.getPropertySources().addLast(source);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading config import object from imports/", e);
        }
    }
}

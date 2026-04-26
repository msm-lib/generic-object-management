package com.msm.core.objects.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value(value = "${server.port}")
    private long port;
    @Value(value = "${server.servlet.context-path}")
    private String contextPath;
    private final OpenApiAppIdParameterBuilder appIdParameterBuilder;

    public SwaggerConfig(OpenApiAppIdParameterBuilder appIdParameterBuilder) {
        this.appIdParameterBuilder = appIdParameterBuilder;
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .addOperationCustomizer(appIdParameterBuilder)
                .build();
    }

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // Define error response schema
        Schema<?> errorSchema = new ObjectSchema()
                .addProperty("error", new Schema<>().type("string").example("030599"))
                .addProperty("message", new Schema<>().type("string").example("Technical Error"));

        Schema<?> errorResponseSchema = new ObjectSchema()
                .addProperty("errors", new Schema<>().type("array").items(errorSchema));
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(String.format("http://localhost:%d%s", port, contextPath)),
                        new Server().url(String.format("https://api-dev.digiretail.myminds.net%s", contextPath)),
                        new Server().url(String.format("https://api-qc.digiretail.myminds.net%s", contextPath))
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .addSecurityItem(new SecurityRequirement().addList("X-App-Id"))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))

                        .addSchemas("ErrorResponse", errorResponseSchema)
                        .addSchemas("Error", errorSchema));

    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> openApi.getPaths()
                .forEach((path, pathItem) -> pathItem.readOperations().forEach(operation -> operation.getResponses().addApiResponse("400",
                            new ApiResponse()
                                    .description("Bad Request")
                                    .content(new Content()
                                            .addMediaType("application/json",
                                                    new MediaType().schema(
                                                            new Schema<>().$ref(
                                                                    "#/components/schemas/ErrorResponse")))))
                ));
    }
}

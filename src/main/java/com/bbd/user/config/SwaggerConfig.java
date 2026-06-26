package com.bbd.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch(
                        "/api/**",
                        "/scim/**",
                        "/health",
                        "/actuator/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi erpApi() {
        return GroupedOpenApi.builder()
                .group("erp")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/v1/users/internal/**")
                .build();
    }

    @Bean
    public GroupedOpenApi scimApi() {
        return GroupedOpenApi.builder()
                .group("scim")
                .pathsToMatch("/scim/**")
                .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("internal")
                .pathsToMatch(
                        "/api/v1/users/internal/**",
                        "/health",
                        "/actuator/**"
                )
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("User API")
                        .description("User Application API Documentation")
                        .version("v1.0"))
                .addServersItem(new Server()
                        .url("http://localhost:8081/user")
                        .description("Local 내 컴퓨터"))

                .addServersItem(new Server()
                        .url("http://100.73.142.41/user")
                        .description("강의실 노트북"))

                .addServersItem(new Server()
                        .url("http://192.168.200.220/user")
                        .description("Nginx"))
                .addServersItem(new Server()
                        .url("https://bbd.inwoohub.com/user")
                        .description("inwoohub"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME));
    }
}

package com.queueflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queueFlowOpenAPI() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth))
                .info(new Info()
                        .title("QueueFlow API")
                        .version("v1")
                        .description(
                                "REST API for QueueFlow, a smart queue and appointment "
                                        + "management platform."
                        )
                        .contact(new Contact()
                                .name("QueueFlow API Team"))
                        .license(new License()
                                .name("MIT License")));
    }
}

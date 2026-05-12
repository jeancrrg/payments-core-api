package com.billing.payments_core_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI paymentsCoreOpenApi() {
        return new OpenAPI()
                .info(buildInfo())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, buildBearerScheme()));
    }

    private Info buildInfo() {
        return new Info()
                .title("payments-core API")
                .version("v1")
                .description("""
                    Microserviço de orquestração de pagamentos integrado ao gateway Stripe.
                    Inclui retry com Resilience4j, cache distribuído com Redis e processamento
                    assíncrono via @Async do Spring.
                """);
    }

    private SecurityScheme buildBearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Informe o token JWT obtido em POST /v1/auth/login");
    }

}

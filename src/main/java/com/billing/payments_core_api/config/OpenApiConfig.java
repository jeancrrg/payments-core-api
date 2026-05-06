package com.billing.payments_core_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentsCoreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("payments-core API")
                        .description("Microserviço de orquestração de pagamentos integrado ao gateway Stripe. " +
                                "Inclui retry com Resilience4j, cache distribuído com Redis e processamento " +
                                "assíncrono via @Async do Spring.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Billing Platform Team")
                                .email("billing-team@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://payments.example.com").description("Production")
                ));
    }
}

package com.billing.payments_core_api.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new payment via Stripe")
public record CreatePaymentRequest(

        @Schema(description = "Customer identifier in the billing system", example = "cust_12345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "customerId must not be blank")
        @Size(max = 64)
        String customerId,

        @Schema(description = "Payment amount (positive value with up to 2 decimal places)", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "ISO 4217 currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @Schema(description = "Payment method id from Stripe (pm_card_visa for tests)", example = "pm_card_visa")
        String paymentMethodId,

        @Schema(description = "Optional human-readable description", example = "Subscription monthly fee")
        @Size(max = 512)
        String description
) {
}

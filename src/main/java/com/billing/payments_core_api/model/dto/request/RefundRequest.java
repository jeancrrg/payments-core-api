package com.billing.payments_core_api.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request payload for issuing a refund against a Stripe payment")
public record RefundRequest(

        @Schema(description = "ID of the payment to refund", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "paymentId is required")
        UUID paymentId,

        @Schema(description = "Amount to refund. Omit to refund the full payment.", example = "50.00")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Reason for refund", example = "requested_by_customer")
        @Size(max = 256)
        String reason
) {
}

package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Payment representation returned by the API")
public record PaymentResponse(

        @Schema(description = "Internal payment identifier")
        UUID id,

        @Schema(description = "Customer identifier")
        String customerId,

        @Schema(description = "Stripe PaymentIntent identifier", example = "pi_3MtwBwLkdIwHu7ix28a3tqPa")
        String stripePaymentIntentId,

        @Schema(description = "Payment amount")
        BigDecimal amount,

        @Schema(description = "ISO 4217 currency code", example = "USD")
        String currency,

        @Schema(description = "Current payment status")
        PaymentStatus status,

        @Schema(description = "Optional description")
        String description,

        @Schema(description = "Failure reason, if any")
        String failureReason,

        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp")
        OffsetDateTime updatedAt
) {
}

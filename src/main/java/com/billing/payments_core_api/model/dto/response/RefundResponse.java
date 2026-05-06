package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.entity.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Refund representation returned by the API")
public record RefundResponse(

        @Schema(description = "Internal refund identifier")
        UUID id,

        @Schema(description = "Internal payment identifier this refund relates to")
        UUID paymentId,

        @Schema(description = "Stripe refund identifier", example = "re_3MtwBwLkdIwHu7ix28a3tqPa")
        String stripeRefundId,

        @Schema(description = "Refunded amount")
        BigDecimal amount,

        @Schema(description = "Reason given when the refund was requested")
        String reason,

        @Schema(description = "Current refund status")
        RefundStatus status,

        @Schema(description = "Failure reason, if any")
        String failureReason,

        @Schema(description = "Creation timestamp")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp")
        OffsetDateTime updatedAt
) {
}

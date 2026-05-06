package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        String stripeRefundId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}

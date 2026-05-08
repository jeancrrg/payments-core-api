package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundTransactionResponse(
        UUID id,
        UUID paymentId,
        String stripeRefundId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}

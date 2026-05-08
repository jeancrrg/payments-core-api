package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID customerId,
        String stripePaymentIntentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String description,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}

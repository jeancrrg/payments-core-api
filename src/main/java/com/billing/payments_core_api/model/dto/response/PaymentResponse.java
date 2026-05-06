package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String customerId,
        String stripePaymentIntentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String description,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}

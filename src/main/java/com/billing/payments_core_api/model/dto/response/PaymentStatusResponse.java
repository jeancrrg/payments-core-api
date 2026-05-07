package com.billing.payments_core_api.model.dto.response;

import com.billing.payments_core_api.model.enums.PaymentStatus;

import java.util.UUID;

public record PaymentStatusResponse(UUID id, PaymentStatus status) {
}

package com.billing.payments_core_api.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundTransactionRequest(

        @NotNull(message = "paymentId is required")
        UUID paymentId,

        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 256)
        String reason
) {

}

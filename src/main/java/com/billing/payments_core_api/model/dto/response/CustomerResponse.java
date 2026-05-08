package com.billing.payments_core_api.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String cpf,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

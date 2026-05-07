package com.billing.payments_core_api.model.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String cpf,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

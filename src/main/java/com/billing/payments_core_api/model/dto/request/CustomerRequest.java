package com.billing.payments_core_api.model.dto.request;

import com.billing.payments_core_api.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        @NotBlank(message = "cpf must not be blank")
        @ValidCpf
        String cpf
) {
}

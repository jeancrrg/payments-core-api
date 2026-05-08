package com.billing.payments_core_api.validator;

import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final CustomerRepository customerRepository;

    public void validateCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found: " + customerId + "!");
        }
    }

}

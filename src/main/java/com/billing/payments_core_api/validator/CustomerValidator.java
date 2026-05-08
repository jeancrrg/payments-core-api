package com.billing.payments_core_api.validator;

import com.billing.payments_core_api.exception.ConflictException;
import com.billing.payments_core_api.repository.CustomerRepository;
import com.billing.payments_core_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerValidator {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    public void validateCpfAvailableForCreate(String normalizedCpf) {
        if (customerRepository.existsByCpf(normalizedCpf)) {
            throw new ConflictException("Customer with CPF already exists: " + normalizedCpf + "!");
        }
    }

    public void validateCpfAvailableForUpdate(String normalizedCpf, UUID excludeId) {
        if (customerRepository.existsByCpfAndIdNot(normalizedCpf, excludeId)) {
            throw new ConflictException("CPF already in use by another customer: " + normalizedCpf + "!");
        }
    }

    public void validateHasNoPayments(UUID customerId) {
        if (paymentRepository.existsByCustomerId(customerId)) {
            throw new ConflictException("Cannot delete customer with existing payments: " + customerId + "!");
        }
    }

}

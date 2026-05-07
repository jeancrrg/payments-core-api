package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ConflictException;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.mapper.CustomerMapper;
import com.billing.payments_core_api.model.dto.request.CustomerRequest;
import com.billing.payments_core_api.model.dto.response.CustomerResponse;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.entity.Customer;
import com.billing.payments_core_api.repository.CustomerRepository;
import com.billing.payments_core_api.repository.PaymentRepository;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> findAll(Pageable pageable) {
        return PageResponse.from(customerRepository.findAll(pageable).map(customerMapper::toResponse));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        log.info("Creating customer cpf={}", request.cpf());
        String normalizedCpf = request.cpf().replaceAll("[^0-9]", "");
        if (customerRepository.existsByCpf(normalizedCpf)) {
            throw new ConflictException("Customer with CPF already exists: " + normalizedCpf);
        }
        Customer customer = Customer.builder()
                .name(request.name())
                .cpf(normalizedCpf)
                .build();
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_BY_ID, key = "#id")
    public CustomerResponse update(UUID id, CustomerRequest request) {
        log.info("Updating customer id={}", id);
        Customer customer = getOrThrow(id);
        String normalizedCpf = request.cpf().replaceAll("[^0-9]", "");
        if (customerRepository.existsByCpfAndIdNot(normalizedCpf, id)) {
            throw new ConflictException("CPF already in use by another customer: " + normalizedCpf);
        }
        customer.setName(request.name());
        customer.setCpf(normalizedCpf);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_BY_ID, key = "#id")
    public void delete(UUID id) {
        log.info("Deleting customer id={}", id);
        Customer customer = getOrThrow(id);
        if (paymentRepository.existsByCustomerId(id)) {
            throw new ConflictException("Cannot delete customer with existing payments: " + id);
        }
        customerRepository.delete(customer);
    }

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

}

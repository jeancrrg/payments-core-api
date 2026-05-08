package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.mapper.CustomerMapper;
import com.billing.payments_core_api.model.dto.request.CustomerRequest;
import com.billing.payments_core_api.model.dto.response.CustomerResponse;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.entity.Customer;
import com.billing.payments_core_api.repository.CustomerRepository;
import com.billing.payments_core_api.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CustomerValidator customerValidator;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> findAll(Pageable pageable) {
        return PageResponse.from(customerRepository.findAll(pageable).map(customerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_CUSTOMER_BY_ID, key = "#id")
    public CustomerResponse findById(UUID id) {
        log.debug("m=findById, Cache MISS for customer id={} - querying database!", id);
        return customerMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        log.info("m=created, Creating customer for cpf={}!", request.cpf());

        customerValidator.validateCpfAvailableForCreate(request.cpf());
        Customer customer = Customer.builder()
                .name(request.name())
                .cpf(request.cpf())
                .build();
        Customer saved = customerRepository.save(customer);

        log.info("m=created, Customer for CPF={} successfully saved!", request.cpf());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_BY_ID, key = "#id")
    public CustomerResponse update(UUID id, CustomerRequest request) {
        log.info("m=update, Updating customer for id={}!", id);

        customerValidator.validateCpfAvailableForUpdate(request.cpf(), id);
        Customer customer = getOrThrow(id);
        customer.setName(request.name());
        customer.setCpf(request.cpf());

        Customer updated = customerRepository.save(customer);

        log.info("m=update, Customer for id={} successfully updated!", id);
        return customerMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_BY_ID, key = "#id")
    public void delete(UUID id) {
        log.info("m=delete, Deleting customer for id={}!", id);

        customerValidator.validateHasNoPayments(id);
        Customer customer = getOrThrow(id);
        customerRepository.delete(customer);

        log.info("m=deleted, Customer for id={} successfully deleted!", id);
    }

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id + "!"));
    }

}

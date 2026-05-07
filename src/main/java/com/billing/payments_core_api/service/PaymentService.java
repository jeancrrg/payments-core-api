package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.integration.StripeGatewayClient;
import com.billing.payments_core_api.mapper.PaymentMapper;
import com.billing.payments_core_api.model.dto.request.PaymentRequest;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.dto.response.PaymentStatusResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.repository.CustomerRepository;
import com.billing.payments_core_api.repository.PaymentRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final StripeGatewayClient stripeClient;
    private final PaymentNotificationService notificationService;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#id")
    public PaymentResponse findById(UUID id) {
        log.debug("Cache MISS for payment id={} - querying database", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(UUID id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toStatusResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = RedisConfig.CACHE_CUSTOMER_PAYMENTS,
            key = "#customerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<PaymentResponse> findByCustomer(UUID customerId, Pageable pageable) {
        log.debug("Cache MISS for customer payments customerId={} page={} - querying database",
                customerId, pageable.getPageNumber());
        return PageResponse.from(
                paymentRepository.findByCustomerId(customerId, pageable).map(paymentMapper::toResponse)
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, allEntries = true)
    })
    public PaymentResponse syncWithStripe(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        if (payment.getStripePaymentIntentId() == null) {
            return paymentMapper.toResponse(payment);
        }
        PaymentIntent intent = stripeClient.retrievePaymentIntent(payment.getStripePaymentIntentId());
        PaymentStatus newStatus = mapStripeStatus(intent.getStatus());
        if (newStatus != payment.getStatus()) {
            log.info("Payment {} status changed: {} -> {}", id, payment.getStatus(), newStatus);
            payment.setStatus(newStatus);
            payment = paymentRepository.save(payment);
        }
        return paymentMapper.toResponse(payment);
    }

    public PaymentStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (stripeStatus.toLowerCase()) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_payment_method", "requires_confirmation",
                 "requires_action", "requires_capture" -> PaymentStatus.PENDING;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, key = "#request.customerId()")
    })
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for customer={}, amount={} {}",
                request.customerId(), request.amount(), request.currency());
        if (!customerRepository.existsById(request.customerId())) {
            throw new ResourceNotFoundException("Customer not found: " + request.customerId());
        }
        Payment payment = buildAndSaveInitialPayment(request);
        payment = executeStripeProcessing(payment, request);
        notificationService.notifyPaymentProcessed(payment);
        notificationService.auditPaymentEvent(payment, "PAYMENT_CREATED");
        return paymentMapper.toResponse(payment);
    }

    private Payment buildAndSaveInitialPayment(PaymentRequest request) {
        Payment payment = Payment.builder()
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .build();
        return paymentRepository.save(payment);
    }

    private Payment executeStripeProcessing(Payment payment, PaymentRequest request) {
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        try {
            PaymentIntent intent = stripeClient.createPaymentIntent(
                    request.amount(), request.currency(),
                    request.customerId().toString(), request.paymentMethodId(), request.description());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(mapStripeStatus(intent.getStatus()));
            payment = paymentRepository.save(payment);
            log.info("Payment {} processed, stripeIntent={}, status={}",
                    payment.getId(), intent.getId(), payment.getStatus());
            return payment;
        } catch (RuntimeException ex) {
            return handleStripeError(payment, ex);
        }
    }

    private Payment handleStripeError(Payment payment, RuntimeException ex) {
        log.error("Payment {} failed during Stripe processing: {}", payment.getId(), ex.getMessage());
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        payment.setFailureReason(truncate(ex.getMessage()));
        notificationService.notifyPaymentProcessed(payment);
        throw ex;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 512 ? text : text.substring(0, 512);
    }

    @Transactional(readOnly = true)
    public Payment findEntityById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    public boolean existsByCustomerId(UUID id) {
        return paymentRepository.existsByCustomerId(id);
    }

}

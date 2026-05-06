package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.PaymentNotFoundException;
import com.billing.payments_core_api.integration.stripe.StripeGatewayClient;
import com.billing.payments_core_api.mapper.PaymentMapper;
import com.billing.payments_core_api.model.dto.request.CreatePaymentRequest;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.PaymentStatus;
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
    private final StripeGatewayClient stripeClient;
    private final PaymentNotificationService notificationService;
    private final PaymentMapper paymentMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, key = "#request.customerId()")
    })
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for customer={}, amount={} {}",
                request.customerId(), request.amount(), request.currency());

        Payment payment = Payment.builder()
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            PaymentIntent intent = stripeClient.createPaymentIntent(
                    request.amount(),
                    request.currency(),
                    request.customerId(),
                    request.paymentMethodId(),
                    request.description()
            );

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(mapStripeStatus(intent.getStatus()));
            payment = paymentRepository.save(payment);

            log.info("Payment {} processed, stripeIntent={}, status={}",
                    payment.getId(), intent.getId(), payment.getStatus());
        } catch (RuntimeException ex) {
            log.error("Payment {} failed during Stripe processing: {}", payment.getId(), ex.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(truncate(ex.getMessage(), 512));
            paymentRepository.save(payment);
            // Async notification still fires for failures
            notificationService.notifyPaymentProcessed(payment);
            throw ex;
        }

        // Fire-and-forget side effects on the dedicated executor
        notificationService.notifyPaymentProcessed(payment);
        notificationService.auditPaymentEvent(payment, "PAYMENT_CREATED");

        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#id")
    public PaymentResponse findById(UUID id) {
        log.debug("Cache MISS for payment id={} - querying database", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentStatus getStatus(UUID id) {
        return paymentRepository.findById(id)
                .map(Payment::getStatus)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public Payment findEntityById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = RedisConfig.CACHE_CUSTOMER_PAYMENTS,
            key = "#customerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<PaymentResponse> findByCustomer(String customerId, Pageable pageable) {
        log.debug("Cache MISS for customer payments customerId={} page={} - querying database",
                customerId, pageable.getPageNumber());
        return PageResponse.from(
                paymentRepository.findByCustomerId(customerId, pageable)
                        .map(paymentMapper::toResponse)
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, allEntries = true)
    })
    public PaymentResponse syncWithStripe(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));

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

    public static PaymentStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return PaymentStatus.PENDING;
        return switch (stripeStatus.toLowerCase()) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_payment_method", "requires_confirmation",
                 "requires_action", "requires_capture" -> PaymentStatus.PENDING;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

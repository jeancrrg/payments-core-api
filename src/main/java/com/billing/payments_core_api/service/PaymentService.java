package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.integration.StripeFailureHandler;
import com.billing.payments_core_api.integration.StripeGateway;
import com.billing.payments_core_api.model.mapper.PaymentMapper;
import com.billing.payments_core_api.model.mapper.StripeStatusMapper;
import com.billing.payments_core_api.model.dto.request.PaymentRequest;
import com.billing.payments_core_api.model.dto.response.PageResponse;
import com.billing.payments_core_api.model.dto.response.PaymentResponse;
import com.billing.payments_core_api.model.dto.response.PaymentStatusResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.repository.PaymentRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.validator.PaymentValidator;
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
    private final PaymentMapper paymentMapper;
    private final PaymentValidator paymentValidator;
    private final PaymentNotificationService notificationService;
    private final StripeStatusMapper stripeStatusMapper;
    private final StripeGateway stripeGateway;
    private final StripeFailureHandler<Payment> stripeFailureHandler;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#id")
    public PaymentResponse findById(UUID id) {
        log.debug("m=findById, Cache MISS for payment id={} - querying database!", id);
        return paymentMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(UUID id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toStatusResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id + "!"));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS,
                key = "#customerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<PaymentResponse> findByCustomer(UUID customerId, Pageable pageable) {
        log.debug("m=findByCustomer, Cache MISS for customer payments customerId={} page={} - querying database!",
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
        log.info("m=syncWithStripe, Syncing payment={} with Stripe!", id);

        Payment payment = getOrThrow(id);
        if (payment.getStripePaymentIntentId() == null) {
            return paymentMapper.toResponse(payment);
        }
        PaymentIntent intent = stripeGateway.retrievePaymentIntent(payment.getStripePaymentIntentId());
        PaymentStatus newStatus = stripeStatusMapper.toPaymentStatus(intent.getStatus());

        if (newStatus != payment.getStatus()) {
            log.info("m=syncWithStripe, Payment={} status changed: {} -> {}!", id, payment.getStatus(), newStatus);
            payment.setStatus(newStatus);
            payment = paymentRepository.save(payment);
        }

        log.info("m=syncWithStripe, Payment={} synced successfully!", id);
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, key = "#request.customerId()")})
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("m=createPayment, Creating payment for customer={}!", request.customerId());
        paymentValidator.validateCustomerExists(request.customerId());

        Payment payment = paymentRepository.save(paymentMapper.toEntity(request));
        payment = executeStripeProcessing(payment, request);

        notificationService.notifyPaymentProcessed(payment);
        notificationService.auditPaymentEvent(payment, "PAYMENT_CREATED");

        log.info("m=createPayment, Payment created for customer={} successfully!", request.customerId());
        return paymentMapper.toResponse(payment);
    }

    private Payment executeStripeProcessing(Payment payment, PaymentRequest request) {
        try {
            PaymentIntent intent = stripeGateway.createPaymentIntent(request.amount(), request.currency(),
                    request.customerId().toString(), request.paymentMethodId(), request.description());

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(stripeStatusMapper.toPaymentStatus(intent.getStatus()));
            Payment saved = paymentRepository.save(payment);

            log.info("m=executeStripeProcessing, Payment={} processed, stripeIntent={}, status={}!",
                    payment.getId(), intent.getId(), payment.getStatus());
            return saved;
        } catch (ExternalServiceException ex) {
            stripeFailureHandler.handle(payment, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Payment findEntityById(UUID id) {
        return getOrThrow(id);
    }

    private Payment getOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id + "!"));
    }

}

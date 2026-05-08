package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.integration.StripeFailureHandler;
import com.billing.payments_core_api.integration.StripeGateway;
import com.billing.payments_core_api.model.mapper.RefundMapper;
import com.billing.payments_core_api.model.mapper.StripeStatusMapper;
import com.billing.payments_core_api.model.dto.request.RefundRequest;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.Refund;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.model.enums.RefundStatus;
import com.billing.payments_core_api.repository.RefundRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.validator.RefundValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundMapper refundMapper;
    private final RefundValidator refundValidator;
    private final PaymentService paymentService;
    private final PaymentNotificationService notificationService;
    private final StripeStatusMapper stripeStatusMapper;
    private final StripeGateway stripeGateway;
    private final StripeFailureHandler<Refund> stripeFailureHandler;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_REFUND_BY_ID, key = "#id")
    public RefundResponse findById(UUID id) {
        log.debug("m=findById, Cache MISS for refund id={} - querying database!", id);
        return refundMapper.toRefundResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> findByPaymentId(UUID paymentId) {
        return refundMapper.toRefundResponseList(refundRepository.findByPaymentId(paymentId));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#request.paymentId()"),
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, allEntries = true)
    })
    public RefundResponse requestRefund(RefundRequest request) {
        Payment payment = paymentService.findEntityById(request.paymentId());
        refundValidator.validateRefundEligible(payment);
        BigDecimal alreadyRefunded = getAlreadyRefunded(payment.getId());
        BigDecimal refundAmount = resolveRefundAmount(request, payment, alreadyRefunded);
        refundValidator.validateRefundAmounts(refundAmount, alreadyRefunded, payment.getAmount());
        Refund refund = refundRepository.save(refundMapper.toEntity(payment.getId(), refundAmount, request.reason()));
        executeStripeRefund(refund, payment, refundAmount, alreadyRefunded);
        notificationService.notifyRefundProcessed(refund);
        return refundMapper.toRefundResponse(refund);
    }

    private BigDecimal getAlreadyRefunded(UUID paymentId) {
        BigDecimal sum = refundRepository.sumRefundedAmountForPayment(paymentId, RefundStatus.FAILED);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BigDecimal resolveRefundAmount(RefundRequest request, Payment payment, BigDecimal alreadyRefunded) {
        return request.amount() != null
                ? request.amount()
                : payment.getAmount().subtract(alreadyRefunded);
    }

    private void executeStripeRefund(Refund refund, Payment payment, BigDecimal refundAmount, BigDecimal alreadyRefunded) {
        try {
            com.stripe.model.Refund stripeRefund = stripeGateway.createRefund(
                    payment.getStripePaymentIntentId(), refundAmount, payment.getCurrency(), refund.getReason());

            refund.setStripeRefundId(stripeRefund.getId());
            refund.setStatus(stripeStatusMapper.toRefundStatus(stripeRefund.getStatus()));

            refundRepository.save(refund);
            updatePaymentRefundStatus(payment, alreadyRefunded.add(refundAmount));
        } catch (ExternalServiceException ex) {
            stripeFailureHandler.handle(refund, ex);
            throw ex;
        }
    }

    private void updatePaymentRefundStatus(Payment payment, BigDecimal totalRefunded) {
        if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
    }

    private Refund getOrThrow(UUID id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id + "!"));
    }

}

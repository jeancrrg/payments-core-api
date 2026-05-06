package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.InvalidRefundException;
import com.billing.payments_core_api.exception.RefundNotFoundException;
import com.billing.payments_core_api.integration.stripe.StripeGatewayClient;
import com.billing.payments_core_api.mapper.PaymentMapper;
import com.billing.payments_core_api.model.dto.request.RefundRequest;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.PaymentStatus;
import com.billing.payments_core_api.model.entity.Refund;
import com.billing.payments_core_api.model.entity.RefundStatus;
import com.billing.payments_core_api.repository.RefundRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
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
    private final PaymentService paymentService;
    private final StripeGatewayClient stripeClient;
    private final PaymentNotificationService notificationService;
    private final PaymentMapper paymentMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#request.paymentId()"),
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, allEntries = true)
    })
    public RefundResponse requestRefund(RefundRequest request) {
        Payment payment = paymentService.findEntityById(request.paymentId());

        validateRefundEligible(payment);

        BigDecimal alreadyRefunded = refundRepository.sumRefundedAmountForPayment(payment.getId());
        if (alreadyRefunded == null) alreadyRefunded = BigDecimal.ZERO;
        BigDecimal refundAmount = request.amount() != null ? request.amount() : payment.getAmount().subtract(alreadyRefunded);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRefundException("Refund amount must be greater than zero");
        }
        BigDecimal totalAfter = alreadyRefunded.add(refundAmount);
        if (totalAfter.compareTo(payment.getAmount()) > 0) {
            throw new InvalidRefundException(
                    "Refund amount " + refundAmount + " exceeds remaining refundable amount " +
                            payment.getAmount().subtract(alreadyRefunded));
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(refundAmount)
                .reason(request.reason())
                .status(RefundStatus.PENDING)
                .build();
        refund = refundRepository.save(refund);

        try {
            com.stripe.model.Refund stripeRefund = stripeClient.createRefund(
                    payment.getStripePaymentIntentId(),
                    refundAmount,
                    payment.getCurrency(),
                    request.reason()
            );

            refund.setStripeRefundId(stripeRefund.getId());
            refund.setStatus(mapStripeRefundStatus(stripeRefund.getStatus()));
            refund = refundRepository.save(refund);

            // Update parent payment status based on cumulative refund
            BigDecimal totalRefunded = totalAfter;
            if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        } catch (RuntimeException ex) {
            log.error("Refund {} failed: {}", refund.getId(), ex.getMessage());
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(truncate(ex.getMessage(), 512));
            refundRepository.save(refund);
            notificationService.notifyRefundProcessed(refund);
            throw ex;
        }

        notificationService.notifyRefundProcessed(refund);
        return paymentMapper.toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_REFUND_BY_ID, key = "#id")
    public RefundResponse findById(UUID id) {
        log.debug("Cache MISS for refund id={} - querying database", id);
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new RefundNotFoundException("Refund not found: " + id));
        return paymentMapper.toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> findByPaymentId(UUID paymentId) {
        return paymentMapper.toRefundResponseList(refundRepository.findByPaymentId(paymentId));
    }

    private void validateRefundEligible(Payment payment) {
        if (payment.getStripePaymentIntentId() == null) {
            throw new InvalidRefundException("Payment has no associated Stripe transaction and cannot be refunded");
        }
        PaymentStatus s = payment.getStatus();
        if (s == PaymentStatus.PENDING || s == PaymentStatus.PROCESSING || s == PaymentStatus.FAILED || s == PaymentStatus.CANCELLED) {
            throw new InvalidRefundException("Payment in status " + s + " cannot be refunded");
        }
        if (s == PaymentStatus.REFUNDED) {
            throw new InvalidRefundException("Payment is already fully refunded");
        }
    }

    private RefundStatus mapStripeRefundStatus(String stripeStatus) {
        if (stripeStatus == null) return RefundStatus.PENDING;
        return switch (stripeStatus.toLowerCase()) {
            case "succeeded" -> RefundStatus.SUCCEEDED;
            case "pending" -> RefundStatus.PENDING;
            case "canceled" -> RefundStatus.CANCELLED;
            default -> RefundStatus.FAILED;
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

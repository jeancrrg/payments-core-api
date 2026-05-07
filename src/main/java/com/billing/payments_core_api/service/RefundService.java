package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.BusinessException;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.integration.StripeGatewayClient;
import com.billing.payments_core_api.mapper.PaymentMapper;
import com.billing.payments_core_api.model.dto.request.RefundRequest;
import com.billing.payments_core_api.model.dto.response.RefundResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.model.entity.Refund;
import com.billing.payments_core_api.model.enums.RefundStatus;
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
        BigDecimal alreadyRefunded = getAlreadyRefunded(payment.getId());
        BigDecimal refundAmount = resolveRefundAmount(request, payment, alreadyRefunded);
        validateRefundAmounts(refundAmount, alreadyRefunded, payment.getAmount());
        Refund refund = buildAndSaveRefund(payment, refundAmount, request.reason());
        refund = executeStripeRefund(refund, payment, refundAmount, alreadyRefunded);
        notificationService.notifyRefundProcessed(refund);
        return paymentMapper.toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_REFUND_BY_ID, key = "#id")
    public RefundResponse findById(UUID id) {
        log.debug("Cache MISS for refund id={} - querying database", id);
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id));
        return paymentMapper.toRefundResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> findByPaymentId(UUID paymentId) {
        return paymentMapper.toRefundResponseList(refundRepository.findByPaymentId(paymentId));
    }

    private void validateRefundEligible(Payment payment) {
        if (payment.getStripePaymentIntentId() == null) {
            throw new BusinessException("Payment has no associated Stripe transaction and cannot be refunded");
        }
        PaymentStatus s = payment.getStatus();
        if (s == PaymentStatus.PENDING || s == PaymentStatus.PROCESSING
                || s == PaymentStatus.FAILED || s == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment in status " + s + " cannot be refunded");
        }
        if (s == PaymentStatus.REFUNDED) {
            throw new BusinessException("Payment is already fully refunded");
        }
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

    private void validateRefundAmounts(BigDecimal refundAmount, BigDecimal alreadyRefunded, BigDecimal paymentAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Refund amount must be greater than zero");
        }
        BigDecimal totalAfter = alreadyRefunded.add(refundAmount);
        if (totalAfter.compareTo(paymentAmount) > 0) {
            throw new BusinessException(
                    "Refund amount " + refundAmount + " exceeds remaining refundable amount " +
                            paymentAmount.subtract(alreadyRefunded));
        }
    }

    private Refund buildAndSaveRefund(Payment payment, BigDecimal amount, String reason) {
        Refund refund = Refund.builder()
                .payment(payment)
                .amount(amount)
                .reason(reason)
                .status(RefundStatus.PENDING)
                .build();
        return refundRepository.save(refund);
    }

    private Refund executeStripeRefund(Refund refund, Payment payment, BigDecimal refundAmount, BigDecimal alreadyRefunded) {
        try {
            com.stripe.model.Refund stripeRefund = stripeClient.createRefund(
                    payment.getStripePaymentIntentId(), refundAmount,
                    payment.getCurrency(), refund.getReason());
            refund.setStripeRefundId(stripeRefund.getId());
            refund.setStatus(mapStripeRefundStatus(stripeRefund.getStatus()));
            refund = refundRepository.save(refund);
            updatePaymentRefundStatus(payment, alreadyRefunded.add(refundAmount));
            return refund;
        } catch (RuntimeException ex) {
            return handleRefundError(refund, ex);
        }
    }

    private void updatePaymentRefundStatus(Payment payment, BigDecimal totalRefunded) {
        if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
    }

    private Refund handleRefundError(Refund refund, RuntimeException ex) {
        log.error("Refund {} failed: {}", refund.getId(), ex.getMessage());
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureReason(truncate(ex.getMessage()));
        refundRepository.save(refund);
        notificationService.notifyRefundProcessed(refund);
        throw ex;
    }

    private RefundStatus mapStripeRefundStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return RefundStatus.PENDING;
        }
        return switch (stripeStatus.toLowerCase()) {
            case "succeeded" -> RefundStatus.SUCCEEDED;
            case "pending" -> RefundStatus.PENDING;
            case "canceled" -> RefundStatus.CANCELLED;
            default -> RefundStatus.FAILED;
        };
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 512 ? text : text.substring(0, 512);
    }

}

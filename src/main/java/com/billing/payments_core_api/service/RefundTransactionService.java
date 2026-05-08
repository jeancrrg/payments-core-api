package com.billing.payments_core_api.service;

import com.billing.payments_core_api.config.RedisConfig;
import com.billing.payments_core_api.exception.ExternalServiceException;
import com.billing.payments_core_api.exception.ResourceNotFoundException;
import com.billing.payments_core_api.integration.StripeFailureHandler;
import com.billing.payments_core_api.integration.StripeGateway;
import com.billing.payments_core_api.model.mapper.RefundTransactionMapper;
import com.billing.payments_core_api.model.mapper.StripeStatusMapper;
import com.billing.payments_core_api.model.dto.request.RefundTransactionRequest;
import com.billing.payments_core_api.model.dto.response.RefundTransactionResponse;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.entity.RefundTransaction;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.model.enums.RefundStatus;
import com.billing.payments_core_api.repository.RefundTransactionRepository;
import com.billing.payments_core_api.service.async.PaymentNotificationService;
import com.billing.payments_core_api.validator.RefundTransactionValidator;
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
public class RefundTransactionService {

    private final RefundTransactionRepository refundTransactionRepository;
    private final RefundTransactionMapper refundTransactionMapper;
    private final RefundTransactionValidator refundTransactionValidator;
    private final PaymentService paymentService;
    private final PaymentNotificationService notificationService;
    private final StripeStatusMapper stripeStatusMapper;
    private final StripeGateway stripeGateway;
    private final StripeFailureHandler<RefundTransaction> stripeFailureHandler;

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_REFUND_BY_ID, key = "#id")
    public RefundTransactionResponse findById(UUID id) {
        log.debug("m=findById, Cache MISS for refund transaction id={} - querying database!", id);
        return refundTransactionMapper.toRefundTransactionResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RefundTransactionResponse> findByPaymentId(UUID paymentId) {
        return refundTransactionMapper.toRefundTransactionResponseList(refundTransactionRepository.findByPaymentId(paymentId));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PAYMENT_BY_ID, key = "#request.paymentId()"),
            @CacheEvict(value = RedisConfig.CACHE_CUSTOMER_PAYMENTS, allEntries = true)
    })
    public RefundTransactionResponse requestRefund(RefundTransactionRequest request) {
        Payment payment = paymentService.findEntityById(request.paymentId());
        refundTransactionValidator.validateRefundEligible(payment);

        BigDecimal alreadyRefunded = getAlreadyRefunded(payment.getId());
        BigDecimal refundAmount = resolveRefundAmount(request, payment, alreadyRefunded);
        refundTransactionValidator.validateRefundAmounts(refundAmount, alreadyRefunded, payment.getAmount());

        RefundTransaction refundTransaction = createPendingRefund(payment.getId(), refundAmount, request.reason());
        processStripeRefund(refundTransaction, payment, refundAmount, alreadyRefunded);
        notificationService.notifyRefundProcessed(refundTransaction);

        return refundTransactionMapper.toRefundTransactionResponse(refundTransaction);
    }

    private RefundTransaction createPendingRefund(UUID paymentId, BigDecimal amount, String reason) {
        return refundTransactionRepository.save(refundTransactionMapper.toEntity(paymentId, amount, reason));
    }

    private BigDecimal getAlreadyRefunded(UUID paymentId) {
        BigDecimal sum = refundTransactionRepository.sumRefundedAmountForPayment(paymentId, RefundStatus.FAILED);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private BigDecimal resolveRefundAmount(RefundTransactionRequest request, Payment payment, BigDecimal alreadyRefunded) {
        return request.amount() != null
                ? request.amount()
                : payment.getAmount().subtract(alreadyRefunded);
    }

    private void processStripeRefund(RefundTransaction refundTransaction, Payment payment, BigDecimal refundAmount, BigDecimal alreadyRefunded) {
        try {
            com.stripe.model.Refund stripeRefund = stripeGateway.createRefund(
                    payment.getStripePaymentIntentId(), refundAmount, payment.getCurrency(), refundTransaction.getReason());
            applyStripeResult(refundTransaction, stripeRefund);
            updatePaymentRefundStatus(payment, alreadyRefunded.add(refundAmount));
        } catch (ExternalServiceException ex) {
            stripeFailureHandler.handle(refundTransaction, ex);
            throw ex;
        }
    }

    private void applyStripeResult(RefundTransaction refundTransaction, com.stripe.model.Refund stripeRefund) {
        refundTransaction.setStripeRefundId(stripeRefund.getId());
        refundTransaction.setStatus(stripeStatusMapper.toRefundStatus(stripeRefund.getStatus()));
        refundTransactionRepository.save(refundTransaction);
    }

    private void updatePaymentRefundStatus(Payment payment, BigDecimal totalRefunded) {
        if (totalRefunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
    }

    private RefundTransaction getOrThrow(UUID id) {
        return refundTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RefundTransaction not found: " + id + "!"));
    }

}

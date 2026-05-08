package com.billing.payments_core_api.validator;

import com.billing.payments_core_api.exception.BusinessException;
import com.billing.payments_core_api.model.entity.Payment;
import com.billing.payments_core_api.model.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RefundTransactionValidator {

    public void validateRefundEligible(Payment payment) {
        if (payment.getStripePaymentIntentId() == null) {
            throw new BusinessException("Payment has no associated Stripe transaction and cannot be refunded!");
        }
        validateRefundableStatus(payment.getStatus());
    }

    public void validateRefundAmounts(BigDecimal refundAmount, BigDecimal alreadyRefunded, BigDecimal paymentAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Refund amount must be greater than zero!");
        }
        BigDecimal totalAfter = alreadyRefunded.add(refundAmount);
        if (totalAfter.compareTo(paymentAmount) > 0) {
            throw new BusinessException(
                    "Refund amount " + refundAmount + " exceeds remaining refundable amount " +
                            paymentAmount.subtract(alreadyRefunded) + "!");
        }
    }

    private void validateRefundableStatus(PaymentStatus status) {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING
                || status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) {
            throw new BusinessException("Payment in status " + status + " cannot be refunded!");
        }
        if (status == PaymentStatus.REFUNDED) {
            throw new BusinessException("Payment is already fully refunded!");
        }
    }

}

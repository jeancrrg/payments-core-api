package com.billing.payments_core_api.model.mapper;

import com.billing.payments_core_api.model.enums.PaymentStatus;
import com.billing.payments_core_api.model.enums.RefundStatus;
import com.billing.payments_core_api.model.enums.StripeStatus;
import org.springframework.stereotype.Component;

@Component
public class StripeStatusMapper {

    public PaymentStatus toPaymentStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PaymentStatus.PENDING;
        }
        StripeStatus status = StripeStatus.fromValue(stripeStatus);
        return switch (status) {
            case PENDING -> PaymentStatus.PENDING;
            case SUCCEEDED -> PaymentStatus.SUCCEEDED;
            case PROCESSING -> PaymentStatus.PROCESSING;
            case REQUIRES_PAYMENT_METHOD,
                 REQUIRES_CONFIRMATION,
                 REQUIRES_ACTION,
                 REQUIRES_CAPTURE -> PaymentStatus.PENDING;
            case CANCELLED -> PaymentStatus.CANCELLED;
        };
    }

    public RefundStatus toRefundStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return RefundStatus.PENDING;
        }
        StripeStatus status = StripeStatus.fromValue(stripeStatus);
        return switch (status) {
            case SUCCEEDED -> RefundStatus.SUCCEEDED;
            case PENDING -> RefundStatus.PENDING;
            case CANCELLED -> RefundStatus.CANCELLED;
            default -> RefundStatus.FAILED;
        };
    }

}

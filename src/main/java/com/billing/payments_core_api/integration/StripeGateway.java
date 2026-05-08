package com.billing.payments_core_api.integration;

import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;

import java.math.BigDecimal;

public interface StripeGateway {

    PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String customerId,
                                     String paymentMethodId, String description);

    PaymentIntent retrievePaymentIntent(String paymentIntentId);

    Refund createRefund(String paymentIntentId, BigDecimal amount, String currency, String reason);

}

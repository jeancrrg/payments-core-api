package com.billing.payments_core_api.integration;

import com.billing.payments_core_api.exception.ExternalServiceException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class StripeGatewayClient implements StripeGateway {

    private static final String RESILIENCE_INSTANCE = "stripeApi";

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String customerId, String paymentMethodId, String description) {
        try {
            PaymentIntentCreateParams params = buildPaymentIntentParams(amount, currency, customerId, paymentMethodId, description);
            log.info("Creating Stripe PaymentIntent | customer={} amount={} {}", customerId, amount, currency);
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed", e);
            throw new ExternalServiceException("Failed to create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new ExternalServiceException("Failed to retrieve PaymentIntent " + paymentIntentId, e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public Refund createRefund(String paymentIntentId, BigDecimal amount, String currency, String reason) {
        try {
            RefundCreateParams params = buildRefundParams(paymentIntentId, amount, currency, reason);
            log.info("Creating Stripe refund | paymentIntent={} amount={}", paymentIntentId, amount);
            return Refund.create(params);
        } catch (StripeException e) {
            throw new ExternalServiceException("Failed to create refund for " + paymentIntentId, e);
        }
    }

    private PaymentIntentCreateParams buildPaymentIntentParams(BigDecimal amount, String currency, String customerId,
                                                               String paymentMethodId, String description) {
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(amount, currency))
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                .setAutomaticPaymentMethods(buildAutoPaymentMethods())
                .putMetadata("internal_customer_id", customerId);
        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            builder.setPaymentMethod(paymentMethodId).setConfirm(true);
        }
        return builder.build();
    }

    private PaymentIntentCreateParams.AutomaticPaymentMethods buildAutoPaymentMethods() {
        return PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                .build();
    }

    private RefundCreateParams buildRefundParams(String paymentIntentId, BigDecimal amount, String currency, String reason) {
        RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);
        if (amount != null) {
            builder.setAmount(toMinorUnits(amount, currency));
        }
        if (reason != null && !reason.isBlank()) {
            builder.setReason(mapReason(reason));
        }
        return builder.build();
    }

    private long toMinorUnits(BigDecimal amount, String currency) {
        if (isZeroDecimalCurrency(currency)) {
            return amount
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private boolean isZeroDecimalCurrency(String currency) {
        return switch (currency.toUpperCase()) {
            case "JPY", "KRW", "VND", "CLP" -> true;
            default -> false;
        };
    }

    private RefundCreateParams.Reason mapReason(String reason) {
        return switch (reason.toLowerCase()) {
            case "duplicate" -> RefundCreateParams.Reason.DUPLICATE;
            case "fraudulent" -> RefundCreateParams.Reason.FRAUDULENT;
            default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        };
    }

}

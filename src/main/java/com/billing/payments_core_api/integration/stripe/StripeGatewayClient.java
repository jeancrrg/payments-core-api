package com.billing.payments_core_api.integration.stripe;

import com.billing.payments_core_api.exception.StripeIntegrationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentConfirmParams;
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
public class StripeGatewayClient {

    private static final String RESILIENCE_INSTANCE = "stripeApi";

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentIntent createPaymentIntent(BigDecimal amount,
                                             String currency,
                                             String customerId,
                                             String paymentMethodId,
                                             String description) {
        try {
            PaymentIntentCreateParams.Builder builder =
                    PaymentIntentCreateParams.builder()
                            .setAmount(toMinorUnits(amount, currency))
                            .setCurrency(currency.toLowerCase())
                            .setDescription(description)
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .setAllowRedirects(
                                                    PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                            )
                                            .build()
                            )
                            .putMetadata("internal_customer_id", customerId);

            if (paymentMethodId != null && !paymentMethodId.isBlank()) {
                builder.setPaymentMethod(paymentMethodId).setConfirm(true);
            }
            PaymentIntentCreateParams params = builder.build();
            log.info("Creating Stripe PaymentIntent | customer={} amount={} {}",
                    customerId, amount, currency);
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed", e);
            throw new StripeIntegrationException("Failed to create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new StripeIntegrationException("Failed to retrieve PaymentIntent " + paymentIntentId, e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                            .setPaymentMethod(paymentMethodId)
                            .build();
            return intent.confirm(params);
        } catch (StripeException e) {
            throw new StripeIntegrationException("Failed to confirm PaymentIntent " + paymentIntentId, e);
        }
    }

    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public Refund createRefund(String paymentIntentId,
                               BigDecimal amount,
                               String currency,
                               String reason) {
        try {
            RefundCreateParams.Builder builder =
                    RefundCreateParams.builder()
                            .setPaymentIntent(paymentIntentId);

            if (amount != null) {
                builder.setAmount(toMinorUnits(amount, currency));
            }

            if (reason != null && !reason.isBlank()) {
                builder.setReason(mapReason(reason));
            }

            log.info("Creating Stripe refund | paymentIntent={} amount={}",
                    paymentIntentId, amount);

            return Refund.create(builder.build());

        } catch (StripeException e) {
            throw new StripeIntegrationException("Failed to create refund for " + paymentIntentId, e);
        }
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

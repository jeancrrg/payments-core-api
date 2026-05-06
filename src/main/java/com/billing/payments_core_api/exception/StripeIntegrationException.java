package com.billing.payments_core_api.exception;

/**
 * Wraps any Stripe SDK failure that escapes the integration layer.
 * Marked as a retryable exception in Resilience4j config.
 */
public class StripeIntegrationException extends RuntimeException {

    public StripeIntegrationException(String message) {
        super(message);
    }

    public StripeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

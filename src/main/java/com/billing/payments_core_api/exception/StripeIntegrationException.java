package com.billing.payments_core_api.exception;

public class StripeIntegrationException extends RuntimeException {

    public StripeIntegrationException(String message) {
        super(message);
    }

    public StripeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

}

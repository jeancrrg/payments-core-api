package com.billing.payments_core_api.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StripeStatus {

    PENDING("pending"),
    SUCCEEDED("succeeded"),
    PROCESSING("processing"),
    REQUIRES_PAYMENT_METHOD("requires_payment_method"),
    REQUIRES_CONFIRMATION("requires_confirmation"),
    REQUIRES_ACTION("requires_action"),
    REQUIRES_CAPTURE("requires_capture"),
    CANCELLED("cancelled");

    private final String value;

    public static StripeStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (StripeStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

}

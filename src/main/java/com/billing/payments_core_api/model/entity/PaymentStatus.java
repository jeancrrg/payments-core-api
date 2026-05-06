package com.billing.payments_core_api.model.entity;

/**
 * Lifecycle states for a payment in the system.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}

-- =============================================================
-- V1: payments table
-- =============================================================
CREATE TABLE payments (
    id                          UUID PRIMARY KEY,
    customer_id                 VARCHAR(64)    NOT NULL,
    stripe_payment_intent_id    VARCHAR(128),
    amount                      NUMERIC(19, 2) NOT NULL,
    currency                    VARCHAR(3)     NOT NULL,
    status                      VARCHAR(32)    NOT NULL,
    description                 VARCHAR(512),
    failure_reason              VARCHAR(512),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_payments_status CHECK (status IN
        ('PENDING','PROCESSING','SUCCEEDED','FAILED','CANCELLED','REFUNDED','PARTIALLY_REFUNDED')),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE UNIQUE INDEX idx_payments_intent_id ON payments (stripe_payment_intent_id) WHERE stripe_payment_intent_id IS NOT NULL;
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);

COMMENT ON TABLE payments IS 'Payments orchestrated through the Stripe gateway';
COMMENT ON COLUMN payments.stripe_payment_intent_id IS 'PaymentIntent id returned by Stripe';

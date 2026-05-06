-- =============================================================
-- V2: refunds table
-- =============================================================
CREATE TABLE refunds (
    id                  UUID PRIMARY KEY,
    payment_id          UUID           NOT NULL,
    stripe_refund_id    VARCHAR(128),
    amount              NUMERIC(19, 2) NOT NULL,
    reason              VARCHAR(256),
    status              VARCHAR(32)    NOT NULL,
    failure_reason      VARCHAR(512),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT chk_refunds_status CHECK (status IN ('PENDING','SUCCEEDED','FAILED','CANCELLED')),
    CONSTRAINT chk_refunds_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
CREATE UNIQUE INDEX idx_refunds_stripe_refund_id ON refunds (stripe_refund_id) WHERE stripe_refund_id IS NOT NULL;
CREATE INDEX idx_refunds_status ON refunds (status);

COMMENT ON TABLE refunds IS 'Refunds requested against payments';

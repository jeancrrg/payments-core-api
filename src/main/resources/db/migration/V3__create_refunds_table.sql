-- Tabela de reembolsos solicitados contra pagamentos existentes
CREATE TABLE refunds (
    id                  UUID           PRIMARY KEY,
    payment_id          UUID           NOT NULL,
    stripe_refund_id    VARCHAR(128),
    amount              NUMERIC(19, 2) NOT NULL,
    reason              VARCHAR(256),
    status              VARCHAR(32)    NOT NULL,
    failure_reason      VARCHAR(512),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
CREATE UNIQUE INDEX idx_refunds_stripe_refund_id ON refunds (stripe_refund_id);
CREATE INDEX idx_refunds_status ON refunds (status);

COMMENT ON TABLE refunds IS 'Reembolsos solicitados contra pagamentos existentes';
COMMENT ON COLUMN refunds.id IS 'Identificador único do reembolso (UUID gerado pela aplicação)';
COMMENT ON COLUMN refunds.payment_id IS 'Referência ao pagamento que originou o reembolso';
COMMENT ON COLUMN refunds.stripe_refund_id IS 'ID do Refund retornado pelo Stripe';
COMMENT ON COLUMN refunds.amount IS 'Valor reembolsado com precisão de duas casas decimais';
COMMENT ON COLUMN refunds.reason IS 'Motivo do reembolso informado pelo solicitante';
COMMENT ON COLUMN refunds.status IS 'Status atual do reembolso (PENDING, SUCCEEDED, FAILED, CANCELLED)';
COMMENT ON COLUMN refunds.failure_reason IS 'Motivo da falha retornado pelo Stripe em caso de erro no reembolso';
COMMENT ON COLUMN refunds.created_at IS 'Data e hora de criação do registro';
COMMENT ON COLUMN refunds.updated_at IS 'Data e hora da última atualização do registro';

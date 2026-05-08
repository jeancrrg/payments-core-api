CREATE TABLE payments (
    id                          UUID           PRIMARY KEY,
    customer_id                 UUID           NOT NULL,
    stripe_payment_intent_id    VARCHAR(128),
    amount                      NUMERIC(19, 2) NOT NULL,
    currency                    VARCHAR(3)     NOT NULL,
    status                      VARCHAR(32)    NOT NULL,
    description                 VARCHAR(512),
    failure_reason              VARCHAR(512),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT
);

CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE UNIQUE INDEX idx_payments_intent_id ON payments (stripe_payment_intent_id);
CREATE INDEX idx_payments_status ON payments (status);

COMMENT ON TABLE payments IS 'Pagamentos orquestrados através do gateway Stripe';
COMMENT ON COLUMN payments.id IS 'Identificador único do pagamento (UUID gerado pela aplicação)';
COMMENT ON COLUMN payments.customer_id IS 'Referência ao cliente que originou o pagamento';
COMMENT ON COLUMN payments.stripe_payment_intent_id IS 'ID do PaymentIntent retornado pelo Stripe';
COMMENT ON COLUMN payments.amount IS 'Valor do pagamento com precisão de duas casas decimais';
COMMENT ON COLUMN payments.currency IS 'Código ISO 4217 da moeda (ex: USD, BRL, EUR)';
COMMENT ON COLUMN payments.status IS 'Status atual do pagamento (PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED)';
COMMENT ON COLUMN payments.description IS 'Descrição livre do pagamento informada pelo cliente';
COMMENT ON COLUMN payments.failure_reason IS 'Motivo da falha retornado pelo Stripe em caso de erro';
COMMENT ON COLUMN payments.created_at IS 'Data e hora de criação do registro';
COMMENT ON COLUMN payments.updated_at IS 'Data e hora da última atualização do registro';

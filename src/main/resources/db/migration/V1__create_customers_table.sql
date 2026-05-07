-- Tabela de clientes cadastrados na plataforma
CREATE TABLE customers (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    cpf         VARCHAR(11)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_customers_cpf UNIQUE (cpf)
);

CREATE INDEX idx_customers_cpf ON customers (cpf);

COMMENT ON TABLE customers IS 'Clientes cadastrados na plataforma de pagamentos';
COMMENT ON COLUMN customers.id IS 'Identificador único do cliente (UUID gerado pela aplicação)';
COMMENT ON COLUMN customers.name IS 'Nome completo do cliente';
COMMENT ON COLUMN customers.cpf IS 'CPF do cliente sem formatação (11 dígitos numéricos)';
COMMENT ON COLUMN customers.created_at IS 'Data e hora de criação do registro';
COMMENT ON COLUMN customers.updated_at IS 'Data e hora da última atualização do registro';

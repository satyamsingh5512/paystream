-- Saga-orchestrated payment transactions.
CREATE TABLE transactions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    source_account_id  UUID NOT NULL,
    dest_account_id    UUID NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3) NOT NULL DEFAULT 'INR',
    state              VARCHAR(32) NOT NULL,
    failure_reason     VARCHAR(512),
    ip_address         VARCHAR(64),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP,
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user ON transactions (user_id);
CREATE INDEX idx_transactions_state ON transactions (state);

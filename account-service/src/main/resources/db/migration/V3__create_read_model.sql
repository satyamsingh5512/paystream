-- CQRS read model: eventually-consistent projection of account balances, updated by
-- consuming balance-updated events from Kafka. Optimised for fast, lock-free reads.
CREATE TABLE account_read_model (
    account_id   UUID PRIMARY KEY,
    owner_id     UUID NOT NULL,
    currency     VARCHAR(3) NOT NULL,
    balance      NUMERIC(19, 4) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_read_model_owner ON account_read_model (owner_id);

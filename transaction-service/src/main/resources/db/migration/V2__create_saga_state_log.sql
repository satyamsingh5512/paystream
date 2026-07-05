-- Append-only audit log of saga state transitions.
CREATE TABLE saga_state_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    state           VARCHAR(32) NOT NULL,
    detail          VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_log_tx ON saga_state_log (transaction_id, created_at);

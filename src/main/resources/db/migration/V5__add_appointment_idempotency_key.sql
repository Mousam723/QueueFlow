ALTER TABLE appointments
    ADD COLUMN idempotency_key VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uq_appointment_idempotency_key
        UNIQUE (idempotency_key);

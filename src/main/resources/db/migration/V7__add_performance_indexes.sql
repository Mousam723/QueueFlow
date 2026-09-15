CREATE INDEX idx_appointment_queue_status_token
    ON appointments (queue_id, status, token_number);

CREATE INDEX idx_appointment_customer_created
    ON appointments (customer_id, created_at);

CREATE INDEX idx_queue_business_date
    ON queues (business_id, queue_date);

CREATE INDEX idx_business_staff_business_created
    ON business_staff (business_id, created_at);

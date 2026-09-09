CREATE TABLE appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    customer_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    queue_id BIGINT NOT NULL,

    token_number INT NOT NULL,
    appointment_date DATE NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_customer
        FOREIGN KEY (customer_id) REFERENCES users(id),

    CONSTRAINT fk_appointment_business
        FOREIGN KEY (business_id) REFERENCES businesses(id),

    CONSTRAINT fk_appointment_service
        FOREIGN KEY (service_id) REFERENCES services(id),

    CONSTRAINT fk_appointment_queue
        FOREIGN KEY (queue_id) REFERENCES queues(id),

    CONSTRAINT uq_appointment_queue_token
        UNIQUE (queue_id, token_number),

    INDEX idx_appointment_customer (customer_id),
    INDEX idx_appointment_business (business_id),
    INDEX idx_appointment_service (service_id),
    INDEX idx_appointment_queue (queue_id),
    INDEX idx_appointment_date (appointment_date),
    INDEX idx_appointment_status (status)
);
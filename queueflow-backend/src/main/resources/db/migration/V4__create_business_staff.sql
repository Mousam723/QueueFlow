CREATE TABLE business_staff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_business_staff_business
        FOREIGN KEY (business_id) REFERENCES businesses(id),

    CONSTRAINT fk_business_staff_staff
        FOREIGN KEY (staff_id) REFERENCES users(id),

    CONSTRAINT uq_business_staff
        UNIQUE (business_id, staff_id),

    INDEX idx_business_staff_business (business_id),
    INDEX idx_business_staff_staff (staff_id)
);

CREATE TABLE IF NOT EXISTS user_credentials (
    user_id BIGINT PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS booking_payment_links (
    booking_id BIGINT PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_booking_payment_links_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_booking_payment_links_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE tracked_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    issue_date DATE,
    expiry_date DATE NOT NULL,
    notes TEXT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tracked_documents_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

CREATE TABLE document_renewals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    previous_expiry_date DATE NOT NULL,
    new_expiry_date DATE NOT NULL,
    renewed_at DATETIME(6) NOT NULL,
    notes TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_document_renewals_document
        FOREIGN KEY (document_id) REFERENCES tracked_documents (id)
) ENGINE=InnoDB;

CREATE TABLE reminder_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATE NOT NULL,
    days_before INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reminder_events_document_expiry_days
        UNIQUE (document_id, expiry_date, days_before),
    CONSTRAINT fk_reminder_events_document
        FOREIGN KEY (document_id) REFERENCES tracked_documents (id),
    CONSTRAINT fk_reminder_events_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS transaction_application;
CREATE TABLE transaction_application (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, application_no VARCHAR(64) NOT NULL UNIQUE,
 applicant_id VARCHAR(32) NOT NULL, applicant_name VARCHAR(64) NOT NULL, department_id VARCHAR(32) NOT NULL,
 mobile_ciphertext CLOB NOT NULL, id_card_ciphertext CLOB NOT NULL, account_ciphertext CLOB NOT NULL,
 counterparty_account_ciphertext CLOB NOT NULL, amount DECIMAL(18,2) NOT NULL, currency VARCHAR(3) NOT NULL,
 purpose VARCHAR(500) NOT NULL, risk_score INT NOT NULL, risk_level VARCHAR(16) NOT NULL,
 status VARCHAR(32) NOT NULL, current_level INT NOT NULL, rejection_reason VARCHAR(500),
 version BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL
);
CREATE TABLE approval_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, transaction_id BIGINT NOT NULL, level INT NOT NULL,
 approver_id VARCHAR(32) NOT NULL, approver_name VARCHAR(64) NOT NULL, action VARCHAR(16) NOT NULL,
 status_before VARCHAR(32) NOT NULL, status_after VARCHAR(32) NOT NULL, comment VARCHAR(500), created_at TIMESTAMP NOT NULL
);
CREATE TABLE audit_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, transaction_id BIGINT NOT NULL, operator_id VARCHAR(32) NOT NULL,
 operator_name VARCHAR(64) NOT NULL, operation VARCHAR(32) NOT NULL, detail VARCHAR(1000) NOT NULL,
 ip_address VARCHAR(64), created_at TIMESTAMP NOT NULL
);

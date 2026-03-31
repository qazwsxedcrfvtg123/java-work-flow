CREATE TABLE workflow_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_request_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason TEXT,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workflow_request_id) REFERENCES workflow_requests(id)
);
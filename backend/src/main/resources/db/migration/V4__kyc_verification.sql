-- V4__kyc_verification.sql
CREATE TABLE verification_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    identity_profile_id BIGINT NOT NULL REFERENCES identity_profiles(id),
    status VARCHAR(50) NOT NULL, -- PENDING, IN_REVIEW, APPROVED, REJECTED, CHANGES_REQUESTED
    admin_id BIGINT REFERENCES users(id),
    notes TEXT,
    rejection_reason TEXT,
    submitted_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE verification_documents (
    request_id BIGINT NOT NULL REFERENCES verification_requests(id),
    document_id BIGINT NOT NULL REFERENCES kyc_documents(id),
    PRIMARY KEY (request_id, document_id)
);

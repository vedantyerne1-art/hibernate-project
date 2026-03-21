-- V3__identity_and_documents.sql
CREATE TABLE identity_profiles (
    id BIGSERIAL PRIMARY KEY,
    identity_number VARCHAR(100) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    dob DATE,
    gender VARCHAR(50),
    address TEXT,
    nationality VARCHAR(100),
    profile_photo_url VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE kyc_documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    identity_profile_id BIGINT REFERENCES identity_profiles(id),
    document_type VARCHAR(50) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_number VARCHAR(100),
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_shared BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    review_remarks TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

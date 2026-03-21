-- V6__advanced_onboarding_locker_and_qr.sql

ALTER TABLE identity_profiles
    ADD COLUMN IF NOT EXISTS father_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mother_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS occupation VARCHAR(255),
    ADD COLUMN IF NOT EXISTS marital_status VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS alternate_phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS current_address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS current_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_district VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_pincode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS current_country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS permanent_same_as_current BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS permanent_address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS permanent_address_line2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS permanent_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS permanent_district VARCHAR(100),
    ADD COLUMN IF NOT EXISTS permanent_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS permanent_pincode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS permanent_country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS onboarding_step VARCHAR(50) NOT NULL DEFAULT 'STEP_1_PERSONAL',
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS onboarding_progress INTEGER NOT NULL DEFAULT 0;

ALTER TABLE kyc_documents
    ADD COLUMN IF NOT EXISTS document_category VARCHAR(50) NOT NULL DEFAULT 'KYC',
    ADD COLUMN IF NOT EXISTS holder_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS issue_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date DATE,
    ADD COLUMN IF NOT EXISTS back_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS back_file_path TEXT;

ALTER TABLE verification_requests
    ADD COLUMN IF NOT EXISTS resubmission_reason TEXT;

CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    identity_profile_id BIGINT NOT NULL REFERENCES identity_profiles(id),
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kyc_documents_user_id_archived ON kyc_documents(user_id, is_archived);
CREATE INDEX IF NOT EXISTS idx_kyc_documents_category ON kyc_documents(document_category);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_token ON verification_tokens(token);

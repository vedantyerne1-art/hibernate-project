-- V7__advanced_identity_vault_and_security.sql

ALTER TABLE identity_profiles
    ADD COLUMN IF NOT EXISTS trust_score INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS identity_level VARCHAR(30) NOT NULL DEFAULT 'LEVEL_1_BASIC',
    ADD COLUMN IF NOT EXISTS risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    ADD COLUMN IF NOT EXISTS last_risk_evaluated_at TIMESTAMP;

ALTER TABLE kyc_documents
    ADD COLUMN IF NOT EXISTS folder_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS tags VARCHAR(500),
    ADD COLUMN IF NOT EXISTS previous_version_id BIGINT,
    ADD COLUMN IF NOT EXISTS version_number INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS superseded BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ocr_extracted_text TEXT,
    ADD COLUMN IF NOT EXISTS ocr_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ocr_dob DATE,
    ADD COLUMN IF NOT EXISTS ocr_document_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS comparison_warning TEXT;

ALTER TABLE kyc_documents
    ADD CONSTRAINT fk_kyc_documents_previous_version
    FOREIGN KEY (previous_version_id)
    REFERENCES kyc_documents(id);

CREATE TABLE IF NOT EXISTS document_folders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    folder_name VARCHAR(150) NOT NULL,
    parent_folder_id BIGINT REFERENCES document_folders(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_folders_user_folder_parent
    ON document_folders(user_id, folder_name, COALESCE(parent_folder_id, -1));

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    session_token VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(120),
    location VARCHAR(255),
    user_agent VARCHAR(1000),
    last_active_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_sessions_session_token ON user_sessions(session_token);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id_revoked ON user_sessions(user_id, revoked);

CREATE TABLE IF NOT EXISTS in_app_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    metadata_json TEXT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON in_app_notifications(user_id, read);

CREATE TABLE IF NOT EXISTS delegated_access (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    delegate_user_id BIGINT NOT NULL REFERENCES users(id),
    permission VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_delegated_access_owner_delegate
    ON delegated_access(owner_user_id, delegate_user_id);

CREATE TABLE IF NOT EXISTS share_links (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    document_ids_csv TEXT,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document_access_logs (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    accessor_user_id BIGINT,
    accessor_email VARCHAR(255),
    document_id BIGINT,
    access_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(120),
    user_agent VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_access_logs_owner_created_at
    ON document_access_logs(owner_user_id, created_at DESC);

-- V5__consent_and_api.sql
CREATE TABLE api_clients (
    id BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL UNIQUE,
    client_key VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    organization_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE user_consents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    api_client_id BIGINT NOT NULL REFERENCES api_clients(id),
    scopes VARCHAR(255) NOT NULL, -- "PROFILE, DOCUMENTS"
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- =========================================================================
-- V1__init_schema.sql
-- SIFAP 2.0 — esquema inicial do monólito modular.
-- Cobre os 4 bounded contexts: beneficiary, admin, payment, audit.
-- Compatível com PostgreSQL 16. REQ-IDs referenciados no SPECIFICATION.md.
-- =========================================================================

-- -------------------------------------------------------------------------
-- Contexto: beneficiary
-- REQ-BEN-001..006
-- -------------------------------------------------------------------------
CREATE TABLE beneficiary (
    id            BIGSERIAL PRIMARY KEY,
    cpf           VARCHAR(11)  NOT NULL,
    name          VARCHAR(120) NOT NULL,
    birth_date    DATE         NOT NULL,
    uf            VARCHAR(2),
    region_code   VARCHAR(2),
    status        VARCHAR(2)   NOT NULL,
    CONSTRAINT uq_beneficiary_cpf UNIQUE (cpf),
    CONSTRAINT ck_beneficiary_status CHECK (status IN ('A','S','C','I','D'))
);

CREATE INDEX idx_beneficiary_status ON beneficiary(status);

CREATE TABLE dependent (
    id              BIGSERIAL PRIMARY KEY,
    beneficiary_id  BIGINT       NOT NULL,
    name            VARCHAR(120) NOT NULL,
    birth_date      DATE         NOT NULL,
    CONSTRAINT fk_dependent_beneficiary FOREIGN KEY (beneficiary_id)
        REFERENCES beneficiary(id) ON DELETE CASCADE
);

CREATE INDEX idx_dependent_beneficiary ON dependent(beneficiary_id);

-- -------------------------------------------------------------------------
-- Contexto: admin (programas sociais)
-- REQ-ADM-001..004
-- -------------------------------------------------------------------------
CREATE TABLE social_program (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(8)     NOT NULL,
    name        VARCHAR(120)   NOT NULL,
    type        VARCHAR(1)     NOT NULL,
    base_value  NUMERIC(14,2)  NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_social_program_code UNIQUE (code),
    CONSTRAINT ck_social_program_type CHECK (type IN ('A','P','T'))
);

-- -------------------------------------------------------------------------
-- Contexto: payment
-- REQ-PAY-001..005
-- -------------------------------------------------------------------------
CREATE TABLE payment (
    id                BIGSERIAL PRIMARY KEY,
    beneficiary_id    BIGINT         NOT NULL,
    beneficiary_cpf   VARCHAR(11)    NOT NULL,
    program_id        BIGINT         NOT NULL,
    cycle             VARCHAR(7)     NOT NULL,
    gross_amount      NUMERIC(14,2)  NOT NULL,
    total_discount    NUMERIC(14,2)  NOT NULL DEFAULT 0,
    thirteenth        NUMERIC(14,2)  NOT NULL DEFAULT 0,
    christmas_bonus   NUMERIC(14,2)  NOT NULL DEFAULT 0,
    net_amount        NUMERIC(14,2)  NOT NULL
);

CREATE INDEX idx_payment_cpf ON payment(beneficiary_cpf);
CREATE INDEX idx_payment_cycle ON payment(cycle);

-- -------------------------------------------------------------------------
-- Contexto: audit
-- REQ-AUD-001 (append-only, ver V2__audit_immutable.sql)
-- REQ-AUD-002 (visibilidade de exclusões EX)
-- -------------------------------------------------------------------------
CREATE TABLE audit_entry (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(32)  NOT NULL,
    entity_id     VARCHAR(64)  NOT NULL,
    action        VARCHAR(2)   NOT NULL,
    state_before  TEXT,
    state_after   TEXT,
    reason        VARCHAR(64),
    cpf_masked    VARCHAR(20),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_action CHECK (action IN ('IN','AL','EX'))
);

CREATE INDEX idx_audit_entity ON audit_entry(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_entry(created_at);

-- =========================================================================
-- V2__audit_immutable.sql
-- REQ-AUD-001: trilha de auditoria imutável (append-only).
-- Tentativas de UPDATE ou DELETE em audit_entry devem ser rejeitadas
-- no nível do banco — defesa em profundidade além da camada de aplicação.
-- =========================================================================

CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_entry is append-only (REQ-AUD-001): % not allowed', TG_OP
        USING ERRCODE = '42501'; -- insufficient_privilege
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_no_update
    BEFORE UPDATE ON audit_entry
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_audit_no_delete
    BEFORE DELETE ON audit_entry
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_modification();

COMMENT ON TABLE audit_entry IS 'Append-only audit trail (REQ-AUD-001). Updates/deletes blocked by trigger.';

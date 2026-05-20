-- REQ-ADM-004 (T01b): externaliza CONSTANTE_K (MYS-003) e persiste valores ajustados do programa.
-- Pré-condição financeira para REQ-PAY-001 / REQ-PAY-005 (ciclo de pagamento).

CREATE TABLE IF NOT EXISTS factor_constants (
    key         TEXT PRIMARY KEY,
    value       NUMERIC(7, 6) NOT NULL,
    min_range   NUMERIC(7, 6) NOT NULL,
    max_range   NUMERIC(7, 6) NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed da constante legada (paridade histórica com CADPROG.NSN#L87).
INSERT INTO factor_constants (key, value, min_range, max_range)
VALUES ('CONSTANTE_K', 0.347215, 0.100000, 0.990000)
ON CONFLICT (key) DO NOTHING;

-- Estende social_program com campos derivados do fator K.
ALTER TABLE social_program
    ADD COLUMN IF NOT EXISTS adjustment_factor   NUMERIC(7, 6),
    ADD COLUMN IF NOT EXISTS factor_k            NUMERIC(12, 10),
    ADD COLUMN IF NOT EXISTS adjusted_base_value NUMERIC(14, 2);

-- Backfill: programas existentes mantêm comportamento atual (sem ajuste) até update explícito.
UPDATE social_program
SET adjustment_factor   = COALESCE(adjustment_factor, 0.000000),
    factor_k            = COALESCE(factor_k, 1.0000000000),
    adjusted_base_value = COALESCE(adjusted_base_value, base_value);

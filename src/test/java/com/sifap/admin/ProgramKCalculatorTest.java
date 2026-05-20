package com.sifap.admin;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T01b — Testes de fator K (REQ-ADM-004).
 * <p>Pré-condição financeira do ciclo: sem fator K aplicado, todos os pagamentos
 * do ciclo divergem do legado (note da spec ativa 002).
 */
class ProgramKCalculatorTest {

    private static final BigDecimal CONSTANTE_K_LEGADO = new BigDecimal("0.347215");

    // covers REQ-ADM-004 — acceptance #1
    @Test
    void legacyConstantWithFactor10ProducesExpectedAdjusted() {
        // 500 × (1 + 0.10 × 0.347215) = 500 × 1.0347215 = 517.36075 → trunca DOWN → 517.36
        ProgramKCalculator.Result r = ProgramKCalculator.compute(
                new BigDecimal("500.00"),
                new BigDecimal("0.10"),
                CONSTANTE_K_LEGADO);
        assertEquals(0, new BigDecimal("1.0347215").compareTo(r.factorK()),
                "fator_k deve ser exatamente 1.0347215");
        assertEquals(new BigDecimal("517.36"), r.adjustedBaseValue(),
                "VLR-BASE-AJUSTADO deve ser 517.36 (truncado, não arredondado)");
    }

    // covers REQ-ADM-004 — acceptance #2 (FATOR-REAJ = 0)
    @Test
    void zeroFactorKeepsBaseValue() {
        ProgramKCalculator.Result r = ProgramKCalculator.compute(
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                CONSTANTE_K_LEGADO);
        assertEquals(0, BigDecimal.ONE.compareTo(r.factorK()), "fator_k deve ser 1.00 quando FATOR-REAJ=0");
        assertEquals(new BigDecimal("500.00"), r.adjustedBaseValue());
    }

    // covers REQ-ADM-004 — acceptance #2 (FATOR-REAJ nulo)
    @Test
    void nullFactorKeepsBaseValue() {
        ProgramKCalculator.Result r = ProgramKCalculator.compute(
                new BigDecimal("750.00"),
                null,
                CONSTANTE_K_LEGADO);
        assertEquals(0, BigDecimal.ONE.compareTo(r.factorK()));
        assertEquals(new BigDecimal("750.00"), r.adjustedBaseValue());
    }

    // REQ-PAY-004 — paridade legada: truncamento DOWN, não arredondamento HALF_UP
    @Test
    void truncatesDownNeverRoundsUp() {
        // 999.99 × 1.0347215 = 1034.71815385 → trunca → 1034.71 (HALF_UP daria 1034.72)
        ProgramKCalculator.Result r = ProgramKCalculator.compute(
                new BigDecimal("999.99"),
                new BigDecimal("0.10"),
                CONSTANTE_K_LEGADO);
        assertEquals(new BigDecimal("1034.71"), r.adjustedBaseValue());
    }

    // REQ-ADM-004 — paridade matemática quando CONSTANTE_K varia
    @Test
    void differentConstantProducesDifferentFactor() {
        // Com CONSTANTE_K=0.50 e FATOR-REAJ=0.20 → fator_k=1.10
        ProgramKCalculator.Result r = ProgramKCalculator.compute(
                new BigDecimal("1000.00"),
                new BigDecimal("0.20"),
                new BigDecimal("0.500000"));
        assertEquals(0, new BigDecimal("1.100000").compareTo(r.factorK()));
        assertEquals(new BigDecimal("1100.00"), r.adjustedBaseValue());
    }

    @Test
    void nullBaseValueOrConstantThrows() {
        assertThrows(NullPointerException.class,
                () -> ProgramKCalculator.compute(null, BigDecimal.ZERO, CONSTANTE_K_LEGADO));
        assertThrows(NullPointerException.class,
                () -> ProgramKCalculator.compute(new BigDecimal("100"), BigDecimal.ZERO, null));
    }
}

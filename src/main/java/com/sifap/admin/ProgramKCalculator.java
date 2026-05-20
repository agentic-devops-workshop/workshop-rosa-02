package com.sifap.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * REQ-ADM-004 — Cálculo do fator K (correção especial do programa social).
 *
 * <p>Fórmula:
 * <pre>
 *   fator_k = 1.00 + (FATOR-REAJ × CONSTANTE_K)
 *   VLR-BASE-AJUSTADO = VLR-BASE × fator_k
 * </pre>
 *
 * <p>Notas de paridade legada (CADPROG.NSN#L86-L92):
 * <ul>
 *   <li>{@code CONSTANTE_K} é externalizada via {@code factor_constants} (MYS-003).</li>
 *   <li>Truncamento de 2 casas com {@link RoundingMode#DOWN} (paridade BR-006).</li>
 *   <li>{@code FATOR-REAJ} nulo ou zero → fator_k = 1.00 → VLR-BASE-AJUSTADO = VLR-BASE.</li>
 * </ul>
 */
public final class ProgramKCalculator {

    /** Resultado imutável do cálculo. */
    public record Result(BigDecimal factorK, BigDecimal adjustedBaseValue) {}

    private ProgramKCalculator() {}

    /**
     * Aplica o fator K sobre o valor base.
     *
     * @param baseValue        VLR-BASE bruto informado no cadastro do programa.
     * @param adjustmentFactor FATOR-REAJ (pode ser {@code null} ou zero).
     * @param constantK        CONSTANTE_K lida de {@code factor_constants}.
     * @return par {@code (factor_k, VLR-BASE-AJUSTADO)} com truncamento de 2 casas.
     */
    public static Result compute(BigDecimal baseValue, BigDecimal adjustmentFactor, BigDecimal constantK) {
        Objects.requireNonNull(baseValue, "baseValue");
        Objects.requireNonNull(constantK, "constantK");

        BigDecimal factor = (adjustmentFactor == null) ? BigDecimal.ZERO : adjustmentFactor;
        // fator_k = 1.00 + (FATOR-REAJ × CONSTANTE_K). Sem truncar — guardamos com precisão original do cálculo.
        BigDecimal factorK = BigDecimal.ONE.add(factor.multiply(constantK));
        BigDecimal adjusted = baseValue.multiply(factorK).setScale(2, RoundingMode.DOWN);
        return new Result(factorK, adjusted);
    }
}

package com.sifap.admin;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * REQ-ADM-004 — Constante financeira externalizada (MYS-003).
 * <p>Substitui a constante mágica {@code 0.347215} hardcoded no legado
 * ({@code CADPROG.NSN#L87}) por uma fonte auditável e parametrizável.
 * <p>Range válido: {@code [0.10, 0.99]} (paridade legada).
 */
@Entity
@Table(name = "factor_constants")
public class FactorConstant {

    public static final String KEY_CONSTANTE_K = "CONSTANTE_K";

    @Id
    @Column(name = "key", length = 32)
    private String key;

    @Column(name = "value", nullable = false, precision = 7, scale = 6)
    private BigDecimal value;

    @Column(name = "min_range", nullable = false, precision = 7, scale = 6)
    private BigDecimal minRange;

    @Column(name = "max_range", nullable = false, precision = 7, scale = 6)
    private BigDecimal maxRange;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getKey() { return key; }
    public void setKey(String v) { this.key = v; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal v) { this.value = v; this.updatedAt = Instant.now(); }
    public BigDecimal getMinRange() { return minRange; }
    public void setMinRange(BigDecimal v) { this.minRange = v; }
    public BigDecimal getMaxRange() { return maxRange; }
    public void setMaxRange(BigDecimal v) { this.maxRange = v; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isInRange(BigDecimal candidate) {
        return candidate.compareTo(minRange) >= 0 && candidate.compareTo(maxRange) <= 0;
    }
}

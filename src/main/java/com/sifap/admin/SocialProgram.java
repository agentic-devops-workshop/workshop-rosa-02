package com.sifap.admin;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "social_program", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class SocialProgram {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8, unique = true)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private ProgramType type;

    /** REQ-ADM-004: valor base bruto informado no cadastro do programa. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal baseValue;

    /** REQ-ADM-004: FATOR-REAJ aplicado. Pode ser nulo (sem ajuste). */
    @Column(name = "adjustment_factor", precision = 7, scale = 6)
    private BigDecimal adjustmentFactor;

    /** REQ-ADM-004: fator_k = 1 + FATOR-REAJ × CONSTANTE_K. */
    @Column(name = "factor_k", precision = 12, scale = 10)
    private BigDecimal factorK;

    /** REQ-ADM-004: VLR-BASE × fator_k. Usado como base de cálculo no ciclo (REQ-PAY-001). */
    @Column(name = "adjusted_base_value", precision = 14, scale = 2)
    private BigDecimal adjustedBaseValue;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public ProgramType getType() { return type; }
    public void setType(ProgramType v) { this.type = v; }
    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal v) { this.baseValue = v; }
    public BigDecimal getAdjustmentFactor() { return adjustmentFactor; }
    public void setAdjustmentFactor(BigDecimal v) { this.adjustmentFactor = v; }
    public BigDecimal getFactorK() { return factorK; }
    public void setFactorK(BigDecimal v) { this.factorK = v; }
    public BigDecimal getAdjustedBaseValue() { return adjustedBaseValue; }
    public void setAdjustedBaseValue(BigDecimal v) { this.adjustedBaseValue = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }

    /**
     * REQ-PAY-001 acceptance #5: base de cálculo do ciclo deve usar o VLR-BASE-AJUSTADO.
     * Faz fallback para {@code baseValue} se ainda não houver ajustado persistido
     * (compatibilidade com programas legados pré-migração).
     */
    public BigDecimal effectivePaymentBase() {
        return adjustedBaseValue != null ? adjustedBaseValue : baseValue;
    }
}

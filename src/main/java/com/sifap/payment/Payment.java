package com.sifap.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "payment", indexes = @Index(name = "idx_payment_cpf", columnList = "beneficiary_cpf"))
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long beneficiaryId;

    @Column(name = "beneficiary_cpf", nullable = false, length = 11)
    private String beneficiaryCpf;

    @Column(nullable = false)
    private Long programId;

    /** Ciclo (ano/mês). */
    @Column(nullable = false, length = 7)
    private String cycle; // YYYY-MM

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    /** Décimo-terceiro (REQ-PAY-003). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal thirteenth = BigDecimal.ZERO;

    /** Abono natalino 15% (REQ-PAY-003). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal christmasBonus = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    public Long getId() { return id; }
    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long v) { this.beneficiaryId = v; }
    public String getBeneficiaryCpf() { return beneficiaryCpf; }
    public void setBeneficiaryCpf(String v) { this.beneficiaryCpf = v; }
    public Long getProgramId() { return programId; }
    public void setProgramId(Long v) { this.programId = v; }
    public String getCycle() { return cycle; }
    public void setCycle(String v) { this.cycle = v; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal v) { this.grossAmount = v; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal v) { this.totalDiscount = v; }
    public BigDecimal getThirteenth() { return thirteenth; }
    public void setThirteenth(BigDecimal v) { this.thirteenth = v; }
    public BigDecimal getChristmasBonus() { return christmasBonus; }
    public void setChristmasBonus(BigDecimal v) { this.christmasBonus = v; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal v) { this.netAmount = v; }

    public YearMonth cycleAsYearMonth() { return YearMonth.parse(cycle); }
}

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

    /** REQ-ADM-004: valor já ajustado pelo FATOR-K. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal baseValue;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public ProgramType getType() { return type; }
    public void setType(ProgramType v) { this.type = v; }
    public BigDecimal getBaseValue() { return baseValue; }
    public void setBaseValue(BigDecimal v) { this.baseValue = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}

package com.sifap.beneficiary;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dependent")
public class Dependent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    public Long getId() { return id; }
    public Beneficiary getBeneficiary() { return beneficiary; }
    public void setBeneficiary(Beneficiary v) { this.beneficiary = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
}

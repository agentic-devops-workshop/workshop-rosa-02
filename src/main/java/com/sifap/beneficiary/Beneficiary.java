package com.sifap.beneficiary;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "beneficiary", uniqueConstraints = @UniqueConstraint(columnNames = "cpf"))
public class Beneficiary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 11, unique = true)
    private String cpf;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 2)
    private String uf;

    /** REQ-ADM-003: código de região (99 = especial). */
    @Column(length = 2)
    private String regionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private BeneficiaryStatus status = BeneficiaryStatus.A;

    @OneToMany(mappedBy = "beneficiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependent> dependents = new ArrayList<>();

    public Long getId() { return id; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
    public String getUf() { return uf; }
    public void setUf(String v) { this.uf = v; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String v) { this.regionCode = v; }
    public BeneficiaryStatus getStatus() { return status; }
    public void setStatus(BeneficiaryStatus v) { this.status = v; }
    public List<Dependent> getDependents() { return dependents; }
}

package com.sifap.beneficiary;

import com.sifap.audit.AuditService;
import com.sifap.common.BrazilianStates;
import com.sifap.common.CpfUtils;
import com.sifap.common.exceptions.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class BeneficiaryService {

    private static final int AGE_AUTO_SUSPEND_THRESHOLD = 75; // REQ-BEN-002
    private static final int MAX_DEPENDENTS = 5;              // REQ-BEN-003

    private final BeneficiaryRepository repository;
    private final AuditService audit;

    public BeneficiaryService(BeneficiaryRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    /** REQ-BEN-001 + REQ-BEN-002 + REQ-BEN-005 + REQ-BEN-006. */
    @Transactional
    public Beneficiary create(BeneficiaryRequest req) {
        // Validação de formação CPF (mod-11). REQ-BEN-006: prefixos especiais passam por bypass dentro de isValid().
        if (!CpfUtils.isValid(req.cpf())) {
            throw BusinessException.badRequest("CPF inválido (falha na regra de formação mod-11)");
        }

        // REQ-BEN-001: CPF único, independente de status.
        if (repository.existsByCpf(req.cpf())) {
            throw BusinessException.conflict("CPF já cadastrado");
        }

        // REQ-BEN-005: valida UF se informado.
        if (req.uf() != null && !req.uf().isBlank() && !BrazilianStates.isValid(req.uf())) {
            throw BusinessException.badRequest("UF inválida");
        }

        // REQ-BEN-006: prefixo especial => documento válido (bypass de validação documental).
        // (Não rejeitamos por CPF formato aqui; a regra documental seria aplicada pelo VALDOCS.)

        Beneficiary b = new Beneficiary();
        b.setCpf(req.cpf());
        b.setName(req.name());
        b.setBirthDate(req.birthDate());
        b.setUf(req.uf());
        b.setRegionCode(req.regionCode());

        // REQ-BEN-002: idade > 75 → status SUSPENDED, ignorando entrada do operador.
        int age = ageOf(req.birthDate());
        BeneficiaryStatus initialStatus = req.status() != null ? req.status() : BeneficiaryStatus.A;
        String autoReason = null;
        if (age > AGE_AUTO_SUSPEND_THRESHOLD) {
            initialStatus = BeneficiaryStatus.S;
            autoReason = "AGE_RULE";
        }
        b.setStatus(initialStatus);

        Beneficiary saved = repository.save(b);

        audit.record("Beneficiary", String.valueOf(saved.getId()), "IN",
                null, "{\"cpf\":\"" + CpfUtils.mask(saved.getCpf()) + "\",\"status\":\"" + saved.getStatus() + "\"}",
                autoReason, saved.getCpf());

        return saved;
    }

    /** REQ-BEN-003 + REQ-BEN-004. */
    @Transactional
    public Dependent addDependent(Long beneficiaryId, DependentRequest req) {
        Beneficiary b = repository.findById(beneficiaryId)
                .orElseThrow(() -> BusinessException.notFound("Beneficiário não encontrado"));

        // REQ-BEN-004: bloqueia inclusão para C ou D (S permitido).
        if (b.getStatus() == BeneficiaryStatus.C || b.getStatus() == BeneficiaryStatus.D) {
            throw BusinessException.unprocessable("Beneficiário " + b.getStatus() + " não aceita dependentes");
        }

        // REQ-BEN-003: limite de 5 dependentes.
        if (b.getDependents().size() >= MAX_DEPENDENTS) {
            throw BusinessException.unprocessable("Limite de dependentes atingido");
        }

        Dependent d = new Dependent();
        d.setBeneficiary(b);
        d.setName(req.name());
        d.setBirthDate(req.birthDate());
        b.getDependents().add(d);
        repository.save(b);

        audit.record("Dependent", String.valueOf(b.getId()), "IN",
                null, "{\"dependentName\":\"" + d.getName() + "\"}", null, b.getCpf());
        return d;
    }

    @Transactional(readOnly = true)
    public List<Beneficiary> findActive() {
        return repository.findByStatus(BeneficiaryStatus.A);
    }

    @Transactional(readOnly = true)
    public Beneficiary findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Beneficiário não encontrado"));
    }

    @Transactional(readOnly = true)
    public java.util.List<Beneficiary> listAll() {
        return repository.findAll();
    }

    private int ageOf(LocalDate birth) {
        return Period.between(birth, LocalDate.now()).getYears();
    }
}

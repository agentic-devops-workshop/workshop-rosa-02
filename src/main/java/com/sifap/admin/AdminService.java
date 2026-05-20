package com.sifap.admin;

import com.sifap.audit.AuditService;
import com.sifap.beneficiary.Beneficiary;
import com.sifap.common.MoneyUtils;
import com.sifap.common.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
public class AdminService {

    /** REQ-ADM-004 (MYS-003): constante FATOR-K externalizável. */
    private final BigDecimal fatorKConstant;
    private final SocialProgramRepository repository;
    private final AuditService audit;

    public AdminService(SocialProgramRepository repository,
                        AuditService audit,
                        @Value("${sifap.admin.fator-k:0.347215}") BigDecimal fatorKConstant) {
        this.repository = repository;
        this.audit = audit;
        this.fatorKConstant = fatorKConstant;
    }

    /** REQ-ADM-001 + REQ-ADM-004. */
    @Transactional
    public SocialProgram create(SocialProgramRequest req) {
        // REQ-ADM-001: COD-PROGRAMA único.
        if (repository.existsByCode(req.code())) {
            throw BusinessException.conflict("COD-PROGRAMA já cadastrado");
        }

        // REQ-ADM-004: VLR-BASE-AJUSTADO = VLR-BASE-INPUT × (1.00 + FATOR-REAJ × 0.347215).
        BigDecimal factor = req.adjustmentFactor() == null ? BigDecimal.ZERO : req.adjustmentFactor();
        BigDecimal multiplier = BigDecimal.ONE.add(factor.multiply(fatorKConstant));
        BigDecimal adjusted = MoneyUtils.truncate2(req.baseValueInput().multiply(multiplier));

        SocialProgram p = new SocialProgram();
        p.setCode(req.code());
        p.setName(req.name());
        p.setType(req.type());
        p.setBaseValue(adjusted);
        p.setActive(true);
        SocialProgram saved = repository.save(p);

        audit.record("SocialProgram", String.valueOf(saved.getId()), "IN",
                null, "{\"code\":\"" + saved.getCode() + "\",\"baseValue\":" + adjusted + "}",
                "FATOR_K_APPLIED", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public SocialProgram findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Programa não encontrado"));
    }

    /** REQ-ADM-002 + REQ-ADM-003. */
    @Transactional(readOnly = true)
    public EligibilityResult checkEligibility(Beneficiary b, SocialProgram program) {
        if (!program.isActive()) {
            return new EligibilityResult(false, "PROGRAM_INACTIVE");
        }

        // REQ-ADM-003: região 99 → aprovação automática.
        if ("99".equals(b.getRegionCode())) {
            audit.record("Eligibility", String.valueOf(b.getId()), "AL",
                    null, "{\"approved\":true}", "REGIAO_ESPECIAL", b.getCpf());
            return new EligibilityResult(true, "REGIAO_ESPECIAL");
        }

        // REQ-ADM-002: faixa etária por tipo.
        int age = Period.between(b.getBirthDate(), LocalDate.now()).getYears();
        switch (program.getType()) {
            case P -> {
                if (age < 60) return new EligibilityResult(false, "IDADE_MIN_P");
            }
            case T -> {
                if (age < 16) return new EligibilityResult(false, "IDADE_MIN_T");
                if (age > 65) return new EligibilityResult(false, "IDADE_MAX_T");
            }
            case A -> { /* sem restrição etária por este requisito */ }
        }
        return new EligibilityResult(true, "OK");
    }
}

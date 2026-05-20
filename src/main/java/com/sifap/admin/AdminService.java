package com.sifap.admin;

import com.sifap.audit.AuditService;
import com.sifap.beneficiary.Beneficiary;
import com.sifap.common.exceptions.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
public class AdminService {

    private final SocialProgramRepository repository;
    private final AuditService audit;
    private final FactorConstantService factorConstantService;

    public AdminService(SocialProgramRepository repository,
                        AuditService audit,
                        FactorConstantService factorConstantService) {
        this.repository = repository;
        this.audit = audit;
        this.factorConstantService = factorConstantService;
    }

    /** REQ-ADM-001 + REQ-ADM-004. */
    @Transactional
    public SocialProgram create(SocialProgramRequest req) {
        // REQ-ADM-001: COD-PROGRAMA único.
        if (repository.existsByCode(req.code())) {
            throw BusinessException.conflict("COD-PROGRAMA já cadastrado");
        }

        // REQ-ADM-004: persistir BASE bruto + factor_k + adjusted_base_value.
        BigDecimal constantK = factorConstantService.getConstantK();
        ProgramKCalculator.Result k = ProgramKCalculator.compute(
                req.baseValueInput(), req.adjustmentFactor(), constantK);

        SocialProgram p = new SocialProgram();
        p.setCode(req.code());
        p.setName(req.name());
        p.setType(req.type());
        p.setBaseValue(req.baseValueInput());
        p.setAdjustmentFactor(req.adjustmentFactor());
        p.setFactorK(k.factorK());
        p.setAdjustedBaseValue(k.adjustedBaseValue());
        p.setActive(true);
        SocialProgram saved = repository.save(p);

        audit.record("SocialProgram", String.valueOf(saved.getId()), "IN",
                null,
                "{\"code\":\"" + saved.getCode()
                        + "\",\"baseValue\":" + saved.getBaseValue()
                        + ",\"adjustmentFactor\":" + saved.getAdjustmentFactor()
                        + ",\"factorK\":" + saved.getFactorK()
                        + ",\"adjustedBaseValue\":" + saved.getAdjustedBaseValue() + "}",
                "FATOR_K_APPLIED", null);
        return saved;
    }

    /**
     * REQ-ADM-004 — acceptance #3: atualização de FATOR-REAJ recalcula factor_k
     * e adjusted_base_value, e emite audit_event PROGRAM_K_UPDATED com antes/depois.
     */
    @Transactional
    public SocialProgram updateAdjustmentFactor(Long programId, BigDecimal newFactor) {
        SocialProgram p = repository.findById(programId)
                .orElseThrow(() -> BusinessException.notFound("Programa não encontrado"));

        BigDecimal previousFactor = p.getAdjustmentFactor();
        BigDecimal previousAdjusted = p.getAdjustedBaseValue();
        BigDecimal previousK = p.getFactorK();

        BigDecimal constantK = factorConstantService.getConstantK();
        ProgramKCalculator.Result k = ProgramKCalculator.compute(
                p.getBaseValue(), newFactor, constantK);

        p.setAdjustmentFactor(newFactor);
        p.setFactorK(k.factorK());
        p.setAdjustedBaseValue(k.adjustedBaseValue());
        SocialProgram saved = repository.save(p);

        String before = "{\"adjustmentFactor\":" + previousFactor
                + ",\"factorK\":" + previousK
                + ",\"adjustedBaseValue\":" + previousAdjusted + "}";
        String after = "{\"adjustmentFactor\":" + saved.getAdjustmentFactor()
                + ",\"factorK\":" + saved.getFactorK()
                + ",\"adjustedBaseValue\":" + saved.getAdjustedBaseValue() + "}";

        audit.record("SocialProgram", String.valueOf(saved.getId()), "AL",
                before, after, "PROGRAM_K_UPDATED", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public SocialProgram findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Programa não encontrado"));
    }

    @Transactional(readOnly = true)
    public java.util.List<SocialProgram> listAll() {
        return repository.findAll();
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

package com.sifap.admin;

import com.sifap.audit.AuditService;
import com.sifap.beneficiary.Beneficiary;
import com.sifap.beneficiary.BeneficiaryStatus;
import com.sifap.common.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private SocialProgramRepository repo;
    private AuditService audit;
    private FactorConstantService factorConstantService;
    private AdminService service;

    @BeforeEach
    void setup() {
        repo = mock(SocialProgramRepository.class);
        audit = mock(AuditService.class);
        factorConstantService = mock(FactorConstantService.class);
        when(factorConstantService.getConstantK()).thenReturn(new BigDecimal("0.347215"));
        service = new AdminService(repo, audit, factorConstantService);
        when(repo.save(any(SocialProgram.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // REQ-ADM-001
    @Test
    void duplicateProgramCodeRejected() {
        when(repo.existsByCode("BPC1")).thenReturn(true);
        SocialProgramRequest r = new SocialProgramRequest("BPC1", "BPC", ProgramType.A,
                new BigDecimal("500.00"), new BigDecimal("0.10"));
        assertThrows(BusinessException.class, () -> service.create(r));
    }

    // REQ-ADM-004 — acceptance #1: factor 0.10 com constante legada produz 517.36.
    @Test
    void fatorKAdjustsBaseValue() {
        SocialProgramRequest r = new SocialProgramRequest("X", "X", ProgramType.A,
                new BigDecimal("500.00"), new BigDecimal("0.10"));
        SocialProgram saved = service.create(r);
        // baseValue agora preserva o input bruto (REQ-ADM-004 #4: ambos persistidos).
        assertEquals(new BigDecimal("500.00"), saved.getBaseValue());
        // adjusted_base_value = 500 * (1 + 0.10 * 0.347215) = 517.36075 → trunca DOWN → 517.36.
        assertEquals(new BigDecimal("517.36"), saved.getAdjustedBaseValue());
        assertEquals(new BigDecimal("0.10"), saved.getAdjustmentFactor());
        // factor_k = 1.0347215.
        assertEquals(0, new BigDecimal("1.0347215").compareTo(saved.getFactorK()));
        // Audit em IN com reason FATOR_K_APPLIED.
        verify(audit).record(eq("SocialProgram"), anyString(), eq("IN"),
                isNull(), anyString(), eq("FATOR_K_APPLIED"), isNull());
    }

    // REQ-ADM-004 — acceptance #2: fator 0 mantém base.
    @Test
    void fatorKZeroKeepsInput() {
        SocialProgramRequest r = new SocialProgramRequest("X", "X", ProgramType.A,
                new BigDecimal("500.00"), BigDecimal.ZERO);
        SocialProgram saved = service.create(r);
        assertEquals(new BigDecimal("500.00"), saved.getBaseValue());
        assertEquals(new BigDecimal("500.00"), saved.getAdjustedBaseValue());
    }

    // REQ-ADM-004 — acceptance #3: update recalcula e audita PROGRAM_K_UPDATED.
    @Test
    void updateAdjustmentFactorRecalculatesAndAudits() {
        SocialProgram existing = new SocialProgram();
        existing.setId(42L);
        existing.setCode("X");
        existing.setName("X");
        existing.setType(ProgramType.A);
        existing.setBaseValue(new BigDecimal("500.00"));
        existing.setAdjustmentFactor(new BigDecimal("0.10"));
        existing.setFactorK(new BigDecimal("1.0347215"));
        existing.setAdjustedBaseValue(new BigDecimal("517.36"));
        existing.setActive(true);
        when(repo.findById(42L)).thenReturn(java.util.Optional.of(existing));

        SocialProgram updated = service.updateAdjustmentFactor(42L, new BigDecimal("0.20"));

        // 500 * (1 + 0.20 * 0.347215) = 500 * 1.0694430 = 534.7215 → trunca DOWN → 534.72.
        assertEquals(new BigDecimal("534.72"), updated.getAdjustedBaseValue());
        assertEquals(new BigDecimal("0.20"), updated.getAdjustmentFactor());
        verify(audit).record(eq("SocialProgram"), eq("42"), eq("AL"),
                anyString(), anyString(), eq("PROGRAM_K_UPDATED"), isNull());
    }

    private Beneficiary beneficiary(int age, String region) {
        Beneficiary b = new Beneficiary();
        b.setCpf("12345678901");
        b.setStatus(BeneficiaryStatus.A);
        b.setBirthDate(LocalDate.now().minusYears(age));
        b.setRegionCode(region);
        return b;
    }

    private SocialProgram program(ProgramType type) {
        SocialProgram p = new SocialProgram();
        p.setCode("X");
        p.setName("X");
        p.setType(type);
        p.setBaseValue(new BigDecimal("100.00"));
        p.setActive(true);
        return p;
    }

    // REQ-ADM-002
    @Test
    void previdenciarioRequires60() {
        assertFalse(service.checkEligibility(beneficiary(59, "01"), program(ProgramType.P)).eligible());
        assertTrue(service.checkEligibility(beneficiary(60, "01"), program(ProgramType.P)).eligible());
    }

    @Test
    void trabalhoAgeRange() {
        assertFalse(service.checkEligibility(beneficiary(15, "01"), program(ProgramType.T)).eligible());
        assertFalse(service.checkEligibility(beneficiary(66, "01"), program(ProgramType.T)).eligible());
        assertTrue(service.checkEligibility(beneficiary(30, "01"), program(ProgramType.T)).eligible());
    }

    // REQ-ADM-003
    @Test
    void region99IsAutoApproved() {
        EligibilityResult r = service.checkEligibility(beneficiary(15, "99"), program(ProgramType.T));
        assertTrue(r.eligible());
        assertEquals("REGIAO_ESPECIAL", r.reason());
    }
}

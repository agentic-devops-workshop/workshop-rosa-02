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
    private AdminService service;

    @BeforeEach
    void setup() {
        repo = mock(SocialProgramRepository.class);
        audit = mock(AuditService.class);
        service = new AdminService(repo, audit, new BigDecimal("0.347215"));
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

    // REQ-ADM-004
    @Test
    void fatorKAdjustsBaseValue() {
        SocialProgramRequest r = new SocialProgramRequest("X", "X", ProgramType.A,
                new BigDecimal("500.00"), new BigDecimal("0.10"));
        SocialProgram saved = service.create(r);
        // 500 * (1 + 0.10 * 0.347215) = 500 * 1.0347215 = 517.36075 -> trunca 517.36
        assertEquals(new BigDecimal("517.36"), saved.getBaseValue());
    }

    // REQ-ADM-004 — fator 0
    @Test
    void fatorKZeroKeepsInput() {
        SocialProgramRequest r = new SocialProgramRequest("X", "X", ProgramType.A,
                new BigDecimal("500.00"), BigDecimal.ZERO);
        SocialProgram saved = service.create(r);
        assertEquals(new BigDecimal("500.00"), saved.getBaseValue());
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

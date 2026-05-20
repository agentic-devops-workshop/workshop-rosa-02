package com.sifap.payment;

import com.sifap.admin.ProgramType;
import com.sifap.admin.SocialProgram;
import com.sifap.admin.SocialProgramRepository;
import com.sifap.audit.AuditService;
import com.sifap.beneficiary.Beneficiary;
import com.sifap.beneficiary.BeneficiaryRepository;
import com.sifap.beneficiary.BeneficiaryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private BeneficiaryRepository beneficiaryRepository;
    private SocialProgramRepository programRepository;
    private AuditService audit;
    private PaymentService service;

    @BeforeEach
    void setup() {
        paymentRepository = mock(PaymentRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        programRepository = mock(SocialProgramRepository.class);
        audit = mock(AuditService.class);
        service = new PaymentService(paymentRepository, beneficiaryRepository, programRepository, audit);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SocialProgram program(ProgramType type, BigDecimal base) {
        SocialProgram p = new SocialProgram();
        p.setCode("BPC1");
        p.setName("BPC");
        p.setType(type);
        p.setBaseValue(base);
        p.setActive(true);
        return p;
    }

    private Beneficiary beneficiary(String cpf, BeneficiaryStatus status) {
        Beneficiary b = new Beneficiary();
        b.setCpf(cpf);
        b.setName("Nome");
        b.setBirthDate(LocalDate.of(1970, 1, 1));
        b.setStatus(status);
        return b;
    }

    private Payment payment(BigDecimal gross) {
        Payment p = new Payment();
        p.setBeneficiaryId(1L);
        p.setBeneficiaryCpf("12345678901");
        p.setProgramId(1L);
        p.setCycle("2026-06");
        p.setGrossAmount(gross);
        p.setTotalDiscount(BigDecimal.ZERO);
        p.setThirteenth(BigDecimal.ZERO);
        p.setChristmasBonus(BigDecimal.ZERO);
        p.setNetAmount(gross);
        return p;
    }

    // REQ-PAY-001
    @Test
    void nonJudicialDiscountIsCappedAt30Percent() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(new BigDecimal("1000.00"))));
        Payment result = service.applyDiscounts(1L,
                List.of(new DiscountRequest(DiscountType.TAX, new BigDecimal("400.00"))));
        assertEquals(new BigDecimal("300.00"), result.getTotalDiscount());
    }

    // REQ-PAY-001
    @Test
    void nonJudicialDiscountExactlyAtCapAccepted() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(new BigDecimal("1000.00"))));
        Payment result = service.applyDiscounts(1L,
                List.of(new DiscountRequest(DiscountType.TAX, new BigDecimal("300.00"))));
        assertEquals(new BigDecimal("300.00"), result.getTotalDiscount());
    }

    // REQ-PAY-002
    @Test
    void judicialDiscountHasNoCap() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(new BigDecimal("1000.00"))));
        Payment result = service.applyDiscounts(1L,
                List.of(new DiscountRequest(DiscountType.J, new BigDecimal("800.00"))));
        assertEquals(new BigDecimal("800.00"), result.getTotalDiscount());
    }

    // REQ-PAY-002 combinação
    @Test
    void mixedDiscountsOnlyNonJudicialIsCapped() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(new BigDecimal("1000.00"))));
        Payment result = service.applyDiscounts(1L, List.of(
                new DiscountRequest(DiscountType.J, new BigDecimal("500.00")),
                new DiscountRequest(DiscountType.TAX, new BigDecimal("400.00"))
        ));
        // judicial 500 + non-judicial capped 300 = 800
        assertEquals(new BigDecimal("800.00"), result.getTotalDiscount());
    }

    // REQ-PAY-003
    @Test
    void decemberAssistencialIncludes13thAndChristmasBonus() {
        SocialProgram prog = program(ProgramType.A, new BigDecimal("1000.00"));
        when(programRepository.findById(1L)).thenReturn(Optional.of(prog));
        when(beneficiaryRepository.findByStatus(BeneficiaryStatus.A))
                .thenReturn(List.of(beneficiary("12345678901", BeneficiaryStatus.A)));

        List<Payment> result = service.generateCycle(YearMonth.of(2026, 12), 1L);

        assertEquals(1, result.size());
        Payment p = result.get(0);
        assertEquals(new BigDecimal("1000.00"), p.getThirteenth());
        assertEquals(new BigDecimal("150.00"), p.getChristmasBonus());
    }

    // REQ-PAY-003 — novembro NÃO inclui
    @Test
    void novemberDoesNotIncludeBonus() {
        SocialProgram prog = program(ProgramType.A, new BigDecimal("1000.00"));
        when(programRepository.findById(1L)).thenReturn(Optional.of(prog));
        when(beneficiaryRepository.findByStatus(BeneficiaryStatus.A))
                .thenReturn(List.of(beneficiary("12345678901", BeneficiaryStatus.A)));
        List<Payment> result = service.generateCycle(YearMonth.of(2026, 11), 1L);
        assertEquals(0, result.get(0).getThirteenth().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.get(0).getChristmasBonus().compareTo(BigDecimal.ZERO));
    }

    // REQ-PAY-003 — programa P em dezembro: sem abono
    @Test
    void decemberPrevidenciarioWithoutBonus() {
        SocialProgram prog = program(ProgramType.P, new BigDecimal("1000.00"));
        when(programRepository.findById(1L)).thenReturn(Optional.of(prog));
        when(beneficiaryRepository.findByStatus(BeneficiaryStatus.A))
                .thenReturn(List.of(beneficiary("12345678901", BeneficiaryStatus.A)));
        List<Payment> result = service.generateCycle(YearMonth.of(2026, 12), 1L);
        assertEquals(0, result.get(0).getChristmasBonus().compareTo(BigDecimal.ZERO));
    }

    // REQ-PAY-005
    @Test
    void cycleGeneratesOnlyForActiveBeneficiariesOrderedByCpf() {
        SocialProgram prog = program(ProgramType.P, new BigDecimal("500.00"));
        when(programRepository.findById(1L)).thenReturn(Optional.of(prog));
        when(beneficiaryRepository.findByStatus(BeneficiaryStatus.A)).thenReturn(List.of(
                beneficiary("33333333333", BeneficiaryStatus.A),
                beneficiary("11111111111", BeneficiaryStatus.A),
                beneficiary("22222222222", BeneficiaryStatus.A)
        ));
        List<Payment> result = service.generateCycle(YearMonth.of(2026, 6), 1L);
        assertEquals(3, result.size());
        assertEquals("11111111111", result.get(0).getBeneficiaryCpf());
        assertEquals("22222222222", result.get(1).getBeneficiaryCpf());
        assertEquals("33333333333", result.get(2).getBeneficiaryCpf());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(3)).save(captor.capture());
    }

    // REQ-PAY-004
    @Test
    void retroactiveCorrectionTruncates() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(new BigDecimal("100.00"))));
        Payment result = service.applyRetroactiveCorrection(1L, new BigDecimal("0.00456"));
        // 100.00 * 1.00456 = 100.456 -> trunca = 100.45
        assertEquals(new BigDecimal("100.45"), result.getGrossAmount());
    }
}

package com.sifap.beneficiary;

import com.sifap.audit.AuditService;
import com.sifap.common.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BeneficiaryServiceTest {

    private BeneficiaryRepository repo;
    private AuditService audit;
    private BeneficiaryService service;

    @BeforeEach
    void setup() {
        repo = mock(BeneficiaryRepository.class);
        audit = mock(AuditService.class);
        service = new BeneficiaryService(repo, audit);
        when(repo.save(any(Beneficiary.class))).thenAnswer(inv -> {
            Beneficiary b = inv.getArgument(0);
            return b;
        });
    }

    // REQ-BEN-001
    @Test
    void duplicateCpfRejected() {
        when(repo.existsByCpf("52998224725")).thenReturn(true);
        BeneficiaryRequest r = new BeneficiaryRequest("52998224725", "X",
                LocalDate.of(1970, 1, 1), "SP", "01", BeneficiaryStatus.A);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r));
        assertEquals(409, ex.getStatus().value());
    }

    // Validação CPF mod-11
    @Test
    void invalidCpfRejected() {
        BeneficiaryRequest r = new BeneficiaryRequest("12345678901", "X",
                LocalDate.of(1970, 1, 1), "SP", "01", BeneficiaryStatus.A);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r));
        assertEquals(400, ex.getStatus().value());
    }

    // REQ-BEN-002
    @Test
    void over75IsAutoSuspended() {
        LocalDate birth = LocalDate.now().minusYears(76);
        BeneficiaryRequest r = new BeneficiaryRequest("52998224725", "X", birth, "SP", "01", BeneficiaryStatus.A);
        Beneficiary saved = service.create(r);
        assertEquals(BeneficiaryStatus.S, saved.getStatus());
    }

    // REQ-BEN-002 limite
    @Test
    void exactly75IsActive() {
        LocalDate birth = LocalDate.now().minusYears(75);
        BeneficiaryRequest r = new BeneficiaryRequest("52998224725", "X", birth, "SP", "01", BeneficiaryStatus.A);
        Beneficiary saved = service.create(r);
        assertEquals(BeneficiaryStatus.A, saved.getStatus());
    }

    // REQ-BEN-005
    @Test
    void invalidUfRejected() {
        BeneficiaryRequest r = new BeneficiaryRequest("52998224725", "X",
                LocalDate.of(1970, 1, 1), "XX", "01", BeneficiaryStatus.A);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r));
        assertEquals(400, ex.getStatus().value());
    }

    // REQ-BEN-003
    @Test
    void over5DependentsRejected() {
        Beneficiary b = new Beneficiary();
        b.setCpf("12345678901");
        b.setStatus(BeneficiaryStatus.A);
        b.setBirthDate(LocalDate.of(1970, 1, 1));
        for (int i = 0; i < 5; i++) {
            Dependent d = new Dependent();
            d.setBeneficiary(b);
            d.setName("d" + i);
            d.setBirthDate(LocalDate.of(2010, 1, 1));
            b.getDependents().add(d);
        }
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addDependent(1L, new DependentRequest("novo", LocalDate.of(2015, 1, 1))));
        assertEquals(422, ex.getStatus().value());
    }

    // REQ-BEN-004
    @Test
    void cancelledBeneficiaryRejectsDependent() {
        Beneficiary b = new Beneficiary();
        b.setCpf("12345678901");
        b.setStatus(BeneficiaryStatus.C);
        b.setBirthDate(LocalDate.of(1970, 1, 1));
        when(repo.findById(1L)).thenReturn(Optional.of(b));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addDependent(1L, new DependentRequest("x", LocalDate.of(2010, 1, 1))));
        assertEquals(422, ex.getStatus().value());
    }
}

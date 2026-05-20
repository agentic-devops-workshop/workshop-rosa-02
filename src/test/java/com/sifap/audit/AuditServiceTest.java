package com.sifap.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

class AuditServiceTest {

    @Test
    void recordsMaskingCpf() { // REQ-AUD-001
        AuditEntryRepository repo = mock(AuditEntryRepository.class);
        when(repo.save(any(AuditEntry.class))).thenAnswer(i -> i.getArgument(0));
        AuditService svc = new AuditService(repo);
        svc.record("Beneficiary", "1", "IN", null, "{}", null, "12345678901");
        verify(repo).save(argThat(e -> e.getCpfMasked() != null && e.getCpfMasked().startsWith("XXX.XXX.")));
    }

    @Test
    void listsIncludeAllActions() { // REQ-AUD-002
        AuditEntryRepository repo = mock(AuditEntryRepository.class);
        AuditEntry ex = new AuditEntry(); ex.setAction("EX");
        AuditEntry in = new AuditEntry(); in.setAction("IN");
        when(repo.findAll()).thenReturn(List.of(in, ex));
        AuditService svc = new AuditService(repo);
        List<AuditEntry> all = svc.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(e -> "EX".equals(e.getAction())));
    }
}

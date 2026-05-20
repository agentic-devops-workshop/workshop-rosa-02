package com.sifap.audit;

import com.sifap.common.CpfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    /** REQ-AUD-001: registra evento imutável. */
    @Transactional
    public void record(String entityType, String entityId, String action,
                       String stateBefore, String stateAfter, String reason, String cpf) {
        AuditEntry e = new AuditEntry();
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setAction(action);
        e.setStateBefore(stateBefore);
        e.setStateAfter(stateAfter);
        e.setReason(reason);
        e.setCpfMasked(CpfUtils.mask(cpf));
        repository.save(e);
    }

    /** REQ-AUD-002: lista TODOS os eventos, inclusive EX (sem filtro automático). */
    @Transactional(readOnly = true)
    public List<AuditEntry> findAll() {
        return repository.findAll();
    }
}

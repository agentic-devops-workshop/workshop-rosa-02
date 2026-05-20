package com.sifap.audit;

import com.sifap.common.exceptions.BusinessException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REQ-AUD-001/REQ-AUD-002: exposição read-only; UPDATE/DELETE bloqueados (403). */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditEntry> list() {
        return service.findAll();
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id) {
        throw BusinessException.forbidden("Audit entries are immutable");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        throw BusinessException.forbidden("Audit entries are immutable");
    }
}

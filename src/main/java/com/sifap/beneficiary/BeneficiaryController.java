package com.sifap.beneficiary;

import com.sifap.common.exceptions.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Beneficiary> create(@RequestBody BeneficiaryRequest req) {
        Beneficiary b = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/beneficiaries/" + b.getId())).body(b);
    }

    @GetMapping("/{id}")
    public Beneficiary get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public java.util.List<Beneficiary> list() {
        return service.listAll();
    }

    @PostMapping("/{id}/dependents")
    public ResponseEntity<Dependent> addDependent(@PathVariable Long id, @RequestBody DependentRequest req) {
        Dependent d = service.addDependent(id, req);
        return ResponseEntity.status(201).body(d);
    }
}

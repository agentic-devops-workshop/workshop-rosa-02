package com.sifap.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/programs")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SocialProgram> create(@RequestBody SocialProgramRequest req) {
        SocialProgram p = service.create(req);
        return ResponseEntity.created(URI.create("/api/v1/admin/programs/" + p.getId())).body(p);
    }

    /** REQ-ADM-004: GET retorna VLR-BASE já ajustado pelo FATOR-K. */
    @GetMapping("/{id}")
    public SocialProgram get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public List<SocialProgram> list() {
        return service.listAll();
    }
}

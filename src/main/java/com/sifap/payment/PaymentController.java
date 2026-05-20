package com.sifap.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /** Gera ciclo de pagamento. REQ-PAY-003/REQ-PAY-005. */
    @PostMapping("/cycles")
    public ResponseEntity<List<Payment>> generateCycle(
            @RequestParam String cycle,           // YYYY-MM
            @RequestParam Long programId) {
        List<Payment> result = service.generateCycle(YearMonth.parse(cycle), programId);
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping
    public List<Payment> listByCycle(@RequestParam String cycle) {
        return service.listByCycle(cycle);
    }

    /** REQ-PAY-001/REQ-PAY-002. */
    @PostMapping("/{id}/discounts")
    public Payment applyDiscounts(@PathVariable Long id, @RequestBody List<DiscountRequest> discounts) {
        return service.applyDiscounts(id, discounts);
    }

    /** REQ-PAY-004. */
    @PostMapping("/{id}/corrections")
    public Payment correction(@PathVariable Long id, @RequestParam BigDecimal factor) {
        return service.applyRetroactiveCorrection(id, factor);
    }
}

package com.sifap.payment;

import com.sifap.admin.ProgramType;
import com.sifap.admin.SocialProgram;
import com.sifap.admin.SocialProgramRepository;
import com.sifap.audit.AuditService;
import com.sifap.beneficiary.Beneficiary;
import com.sifap.beneficiary.BeneficiaryRepository;
import com.sifap.beneficiary.BeneficiaryStatus;
import com.sifap.common.MoneyUtils;
import com.sifap.common.exceptions.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentService {

    /** REQ-PAY-001: teto de 30% para descontos não-judiciais. */
    private static final BigDecimal NON_JUDICIAL_CAP_RATIO = new BigDecimal("0.30");
    /** REQ-PAY-003: abono natalino. */
    private static final BigDecimal CHRISTMAS_BONUS_RATIO = new BigDecimal("0.15");

    private final PaymentRepository paymentRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SocialProgramRepository programRepository;
    private final AuditService audit;

    public PaymentService(PaymentRepository paymentRepository,
                          BeneficiaryRepository beneficiaryRepository,
                          SocialProgramRepository programRepository,
                          AuditService audit) {
        this.paymentRepository = paymentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.programRepository = programRepository;
        this.audit = audit;
    }

    /**
     * REQ-PAY-005: gera ciclo apenas para beneficiários ACTIVE, ordenado por CPF asc.
     * REQ-PAY-003: dezembro + programa assistencial → 13º + abono.
     */
    @Transactional
    public List<Payment> generateCycle(YearMonth cycle, Long programId) {
        SocialProgram program = programRepository.findById(programId)
                .orElseThrow(() -> BusinessException.notFound("Programa não encontrado"));

        // REQ-PAY-005: somente ACTIVE.
        List<Beneficiary> actives = new ArrayList<>(beneficiaryRepository.findByStatus(BeneficiaryStatus.A));
        actives.sort(Comparator.comparing(Beneficiary::getCpf)); // REQ-PAY-005 ordenação por CPF.

        boolean isDecember = cycle.getMonthValue() == 12; // REQ-PAY-003
        List<Payment> created = new ArrayList<>();

        for (Beneficiary b : actives) {
            Payment p = new Payment();
            p.setBeneficiaryId(b.getId());
            p.setBeneficiaryCpf(b.getCpf());
            p.setProgramId(program.getId());
            p.setCycle(cycle.toString()); // YYYY-MM
            BigDecimal gross = program.getBaseValue();
            p.setGrossAmount(MoneyUtils.truncate2(gross));

            // REQ-PAY-003: dezembro + programa tipo A (assistencial).
            if (isDecember && program.getType() == ProgramType.A) {
                p.setThirteenth(MoneyUtils.truncate2(gross));
                p.setChristmasBonus(MoneyUtils.truncate2(gross.multiply(CHRISTMAS_BONUS_RATIO)));
            }

            // Sem descontos no batch padrão. REQ-PAY-004 aplica truncamento.
            BigDecimal net = MoneyUtils.truncate2(
                    p.getGrossAmount().add(p.getThirteenth()).add(p.getChristmasBonus())
                            .subtract(p.getTotalDiscount())
            );
            p.setNetAmount(net);

            created.add(paymentRepository.save(p));
        }

        audit.record("PaymentCycle", cycle.toString(), "IN",
                null, "{\"count\":" + created.size() + ",\"programId\":" + programId + "}",
                "CYCLE_GENERATED", null);
        return created;
    }

    /**
     * REQ-PAY-001 + REQ-PAY-002: aplica descontos com teto 30% para não-judiciais; judicial sem teto.
     * REQ-PAY-004: truncamento 2 casas (FLOOR).
     */
    @Transactional
    public Payment applyDiscounts(Long paymentId, List<DiscountRequest> discounts) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Pagamento não encontrado"));

        BigDecimal gross = p.getGrossAmount();
        BigDecimal cap = MoneyUtils.truncate2(gross.multiply(NON_JUDICIAL_CAP_RATIO));

        BigDecimal nonJudicialRequested = BigDecimal.ZERO;
        BigDecimal judicialTotal = BigDecimal.ZERO;
        for (DiscountRequest d : discounts) {
            if (d.amount() == null || d.amount().signum() < 0) {
                throw BusinessException.badRequest("Desconto inválido");
            }
            if (d.type() == DiscountType.J) {
                judicialTotal = judicialTotal.add(d.amount());
            } else {
                nonJudicialRequested = nonJudicialRequested.add(d.amount());
            }
        }

        // REQ-PAY-001: trunca não-judicial em 30%.
        BigDecimal nonJudicialApplied = nonJudicialRequested.min(cap);
        // REQ-PAY-002: judicial integral.
        BigDecimal totalDiscount = MoneyUtils.truncate2(nonJudicialApplied.add(judicialTotal));
        p.setTotalDiscount(totalDiscount);

        BigDecimal net = MoneyUtils.truncate2(
                p.getGrossAmount().add(p.getThirteenth()).add(p.getChristmasBonus()).subtract(totalDiscount)
        );
        p.setNetAmount(net);

        Payment saved = paymentRepository.save(p);
        audit.record("Payment", String.valueOf(saved.getId()), "AL",
                null, "{\"totalDiscount\":" + totalDiscount + ",\"net\":" + net + "}",
                "DISCOUNTS_APPLIED", saved.getBeneficiaryCpf());
        return saved;
    }

    /** REQ-PAY-004: correção retroativa com truncamento. */
    @Transactional
    public Payment applyRetroactiveCorrection(Long paymentId, BigDecimal correctionFactor) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Pagamento não encontrado"));
        BigDecimal newGross = MoneyUtils.truncate2(p.getGrossAmount().multiply(BigDecimal.ONE.add(correctionFactor)));
        String before = "{\"gross\":" + p.getGrossAmount() + "}";
        p.setGrossAmount(newGross);
        p.setNetAmount(MoneyUtils.truncate2(
                newGross.add(p.getThirteenth()).add(p.getChristmasBonus()).subtract(p.getTotalDiscount())
        ));
        Payment saved = paymentRepository.save(p);
        audit.record("Payment", String.valueOf(saved.getId()), "AL",
                before, "{\"gross\":" + newGross + "}", "RETROACTIVE_CORRECTION", saved.getBeneficiaryCpf());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Payment> listByCycle(String cycle) {
        return paymentRepository.findByCycleOrderByBeneficiaryCpfAsc(cycle);
    }
}

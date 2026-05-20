package com.sifap.admin;

import com.sifap.common.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * REQ-ADM-004 — Lê CONSTANTE_K da tabela {@code factor_constants}.
 * <p>Em ambientes sem Flyway (perfil {@code dev} com H2), aceita um fallback
 * configurável via {@code sifap.admin.fator-k} para manter a paridade legada
 * sem precisar pré-popular a tabela em testes manuais.
 * <p>Em produção (Postgres com migrations), a tabela é semeada pela V3 e o
 * fallback nunca é exercido.
 */
@Service
public class FactorConstantService {

    private final FactorConstantRepository repository;
    private final BigDecimal fallbackConstantK;
    private final BigDecimal fallbackMin;
    private final BigDecimal fallbackMax;

    public FactorConstantService(FactorConstantRepository repository,
                                 @Value("${sifap.admin.fator-k:0.347215}") BigDecimal fallbackConstantK) {
        this.repository = repository;
        this.fallbackConstantK = fallbackConstantK;
        this.fallbackMin = new BigDecimal("0.10");
        this.fallbackMax = new BigDecimal("0.99");
    }

    /**
     * Retorna o valor de CONSTANTE_K, validando o range [0.10, 0.99].
     * @throws BusinessException 500 se o valor persistido estiver fora do range.
     */
    public BigDecimal getConstantK() {
        return repository.findByKey(FactorConstant.KEY_CONSTANTE_K)
                .map(c -> {
                    if (!c.isInRange(c.getValue())) {
                        throw BusinessException.unprocessable(
                                "CONSTANTE_K fora do range [" + c.getMinRange() + ", " + c.getMaxRange() + "]");
                    }
                    return c.getValue();
                })
                .orElseGet(() -> {
                    if (fallbackConstantK.compareTo(fallbackMin) < 0 || fallbackConstantK.compareTo(fallbackMax) > 0) {
                        throw BusinessException.unprocessable(
                                "CONSTANTE_K (fallback) fora do range [0.10, 0.99]");
                    }
                    return fallbackConstantK;
                });
    }
}

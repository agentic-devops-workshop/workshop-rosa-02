package com.sifap.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * REQ-PAY-004: truncamento (FLOOR para positivos) em 2 casas decimais.
 */
public final class MoneyUtils {
    private MoneyUtils() {}

    public static BigDecimal truncate2(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.DOWN);
    }
}

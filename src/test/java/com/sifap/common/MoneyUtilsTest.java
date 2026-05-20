package com.sifap.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** REQ-PAY-004: truncamento (não arredondamento). */
class MoneyUtilsTest {

    @Test
    void truncatesNotRounds() {
        assertEquals(new BigDecimal("100.45"), MoneyUtils.truncate2(new BigDecimal("100.456")));
        assertEquals(new BigDecimal("100.99"), MoneyUtils.truncate2(new BigDecimal("100.999")));
        assertEquals(new BigDecimal("517.36"), MoneyUtils.truncate2(new BigDecimal("517.36075"))); // REQ-ADM-004
    }
}

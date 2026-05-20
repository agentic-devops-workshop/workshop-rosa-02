package com.sifap.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfUtilsTest {

    @Test
    void specialPrefixes() { // REQ-BEN-006
        assertTrue(CpfUtils.isSpecialPrefix("00012345678"));
        assertTrue(CpfUtils.isSpecialPrefix("99912345678"));
        assertFalse(CpfUtils.isSpecialPrefix("12312312300"));
    }

    @Test
    void maskRevealsOnlyTail() { // REQ-AUD-001
        // cpf "12345678901" -> substring(6,9)="789", substring(9,11)="01"
        assertEquals("XXX.XXX.789-01", CpfUtils.mask("12345678901"));
    }
}

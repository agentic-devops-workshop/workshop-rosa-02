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

    @Test
    void mod11AcceptsValidCpf() {
        // CPFs válidos conhecidos pela regra de formação.
        assertTrue(CpfUtils.isValid("52998224725"));
        assertTrue(CpfUtils.isValid("529.982.247-25"), "deve aceitar com máscara");
    }

    @Test
    void mod11RejectsInvalidCheckDigits() {
        assertFalse(CpfUtils.isValid("12345678901"), "DV inválido");
        assertFalse(CpfUtils.isValid("52998224724"), "último DV trocado");
    }

    @Test
    void mod11RejectsAllSameDigits() {
        // Padrão clássico que passa em validadores ingênuos.
        // Usar prefixos NÃO especiais — "000…" e "999…" ficariam no bypass de REQ-BEN-006.
        assertFalse(CpfUtils.isValid("11111111111"));
        assertFalse(CpfUtils.isValid("22222222222"));
        assertFalse(CpfUtils.isValid("88888888888"));
    }

    @Test
    void mod11RejectsWrongLength() {
        assertFalse(CpfUtils.isValid("123"));
        assertFalse(CpfUtils.isValid(""));
        assertFalse(CpfUtils.isValid(null));
    }

    @Test
    void mod11BypassForSpecialPrefixes() { // REQ-BEN-006
        // CPFs com prefixo especial governamental passam mesmo sem DV válido.
        assertTrue(CpfUtils.isValid("00012345678"));
        assertTrue(CpfUtils.isValid("99999999999"));
    }
}

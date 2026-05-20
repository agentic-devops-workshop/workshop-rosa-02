package com.sifap.common;

import java.util.Set;

public final class CpfUtils {
    /** REQ-BEN-006: prefixos especiais governamentais (configuráveis em produção). */
    public static final Set<String> SPECIAL_PREFIXES = Set.of(
            "000", "001", "002", "010", "011", "099", "100", "999"
    );

    private CpfUtils() {}

    public static boolean isSpecialPrefix(String cpf) {
        if (cpf == null || cpf.length() < 3) return false;
        return SPECIAL_PREFIXES.contains(cpf.substring(0, 3));
    }

    /** REQ-AUD-001: máscara XXX.XXX.NNN-NN (revela apenas 5 dígitos do meio/final). */
    public static String mask(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "XXX.XXX." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }
}

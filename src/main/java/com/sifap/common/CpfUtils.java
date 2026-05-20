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

    /** Remove tudo que não é dígito. */
    public static String onlyDigits(String cpf) {
        if (cpf == null) return "";
        return cpf.replaceAll("\\D", "");
    }

    /**
     * Validação CPF — regra de formação (algoritmo mod-11) da Receita Federal.
     *
     * <p>Passos:
     * <ol>
     *   <li>Tem exatamente 11 dígitos.</li>
     *   <li>Não pode ter todos os dígitos iguais (000…000, 111…111 etc.).</li>
     *   <li>1º DV: soma dos 9 primeiros dígitos × pesos 10..2; resto = soma % 11;
     *       DV = (resto &lt; 2) ? 0 : (11 - resto). Deve ser igual ao 10º dígito.</li>
     *   <li>2º DV: soma dos 10 primeiros dígitos × pesos 11..2; mesma regra. Deve ser igual ao 11º.</li>
     * </ol>
     *
     * <p>REQ-BEN-006: CPFs com prefixo especial passam por bypass — retornam {@code true}
     * mesmo sem dígitos verificadores válidos (uso interno do governo legado SIFAP).
     */
    public static boolean isValid(String cpf) {
        if (cpf == null) return false;
        String digits = onlyDigits(cpf);
        if (digits.length() != 11) return false;

        // REQ-BEN-006: bypass para prefixos especiais.
        if (isSpecialPrefix(digits)) return true;

        // Todos dígitos iguais é inválido.
        if (digits.chars().distinct().count() == 1) return false;

        // 1º dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.digit(digits.charAt(i), 10) * (10 - i);
        }
        int rest = sum % 11;
        int dv1 = (rest < 2) ? 0 : (11 - rest);
        if (dv1 != Character.digit(digits.charAt(9), 10)) return false;

        // 2º dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.digit(digits.charAt(i), 10) * (11 - i);
        }
        rest = sum % 11;
        int dv2 = (rest < 2) ? 0 : (11 - rest);
        return dv2 == Character.digit(digits.charAt(10), 10);
    }

    /** REQ-AUD-001: máscara XXX.XXX.NNN-NN (revela apenas 5 dígitos do meio/final). */
    public static String mask(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "XXX.XXX." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }
}

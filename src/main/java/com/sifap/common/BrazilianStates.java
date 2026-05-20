package com.sifap.common;

import java.util.Set;

public final class BrazilianStates {
    public static final Set<String> UFS = Set.of(
            "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
            "PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"
    );
    private BrazilianStates() {}
    public static boolean isValid(String uf) {
        return uf != null && UFS.contains(uf.toUpperCase());
    }
}

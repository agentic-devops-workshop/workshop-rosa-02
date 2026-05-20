package com.sifap.admin;

import java.math.BigDecimal;

public record SocialProgramRequest(
        String code,
        String name,
        ProgramType type,
        BigDecimal baseValueInput,
        BigDecimal adjustmentFactor // FATOR-REAJ
) {}

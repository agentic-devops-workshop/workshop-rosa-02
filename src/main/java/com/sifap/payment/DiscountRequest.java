package com.sifap.payment;

import java.math.BigDecimal;

public record DiscountRequest(DiscountType type, BigDecimal amount) {}

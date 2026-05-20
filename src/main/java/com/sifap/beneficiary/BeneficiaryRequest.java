package com.sifap.beneficiary;

import java.time.LocalDate;

public record BeneficiaryRequest(
        String cpf,
        String name,
        LocalDate birthDate,
        String uf,
        String regionCode,
        BeneficiaryStatus status
) {}

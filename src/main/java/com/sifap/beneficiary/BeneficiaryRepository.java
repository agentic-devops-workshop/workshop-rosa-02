package com.sifap.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    List<Beneficiary> findByStatus(BeneficiaryStatus status);
}

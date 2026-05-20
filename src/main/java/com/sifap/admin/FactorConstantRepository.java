package com.sifap.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FactorConstantRepository extends JpaRepository<FactorConstant, String> {
    Optional<FactorConstant> findByKey(String key);
}

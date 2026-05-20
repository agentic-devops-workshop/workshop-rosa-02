package com.sifap.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SocialProgramRepository extends JpaRepository<SocialProgram, Long> {
    Optional<SocialProgram> findByCode(String code);
    boolean existsByCode(String code);
}

package com.padhora.repository;

import com.padhora.model.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByTutorId(Long tutorId);
}

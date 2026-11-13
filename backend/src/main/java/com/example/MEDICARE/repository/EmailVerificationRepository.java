package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findFirstByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}

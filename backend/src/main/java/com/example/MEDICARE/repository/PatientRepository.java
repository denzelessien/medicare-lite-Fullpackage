package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserEmail(String email);
    Optional<Patient> findByUserId(Long userId);
}

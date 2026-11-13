package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Administration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdministrationRepository extends JpaRepository<Administration, Long> {
    List<Administration> findTop100ByOrderByAdministeredAtDesc();
    List<Administration> findByPatientIdOrderByAdministeredAtDesc(Long patientId);
    List<Administration> findByAdmissionIdOrderByAdministeredAtDesc(Long admissionId);
}

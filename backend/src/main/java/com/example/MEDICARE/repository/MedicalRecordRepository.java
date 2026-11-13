package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByVisitDateDescIdDesc(Long patientId);
    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
}

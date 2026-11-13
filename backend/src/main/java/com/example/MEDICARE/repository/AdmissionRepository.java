package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Admission;
import com.example.MEDICARE.model.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    List<Admission> findByStatusOrderByAdmittedAtDesc(AdmissionStatus status);
    List<Admission> findAllByOrderByAdmittedAtDesc();
    List<Admission> findByAttendingDoctorIdOrderByAdmittedAtDesc(Long doctorId);
    List<Admission> findByAttendingDoctorIdAndStatusOrderByAdmittedAtDesc(Long doctorId, AdmissionStatus status);
    List<Admission> findByWardIdAndStatus(Long wardId, AdmissionStatus status);
    List<Admission> findByPatientIdOrderByAdmittedAtDesc(Long patientId);
    Optional<Admission> findFirstByPatientIdAndStatus(Long patientId, AdmissionStatus status);
    boolean existsByWardId(Long wardId);
    boolean existsByAttendingDoctorIdAndPatientId(Long doctorId, Long patientId);
    long countByStatus(AdmissionStatus status);
    long countByWardIdAndStatus(Long wardId, AdmissionStatus status);
}

package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Payment;
import com.example.MEDICARE.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByAppointmentIdAndStatus(Long appointmentId, PaymentStatus status);
    boolean existsByReference(String reference);
    Optional<Payment> findByReference(String reference);
    long countByStatus(PaymentStatus status);
}

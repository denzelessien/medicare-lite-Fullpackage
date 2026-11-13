package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Appointment;
import com.example.MEDICARE.model.AppointmentStatus;
import com.example.MEDICARE.model.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByAppointmentDateDescAppointmentTimeDesc();
    List<Appointment> findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);
    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByAppointmentDate(LocalDate date);

    List<Appointment> findByStatusAndAppointmentDateLessThanEqual(AppointmentStatus status, LocalDate date);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatus(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus status);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    List<Appointment> findByAppointmentTypeAndStatusAndAdmissionWardIdOrderByAppointmentDateAscAppointmentTimeAsc(
            AppointmentType type, AppointmentStatus status, Long wardId);

    long countByAppointmentDate(LocalDate date);
    long countByStatus(AppointmentStatus status);
    long countByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
}

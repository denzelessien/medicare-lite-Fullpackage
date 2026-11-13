package com.example.MEDICARE.dto;

import com.example.MEDICARE.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Response shapes that combine data from several tables. */
public final class Views {
    private Views() {}

    public record AuthResponse(String token, String role, String fullName, String email,
                               Long userId, Long doctorId, Long patientId, String doctorType) {}

    public record MessageResponse(String message, boolean devMode) {}

    // ---------- wards ----------
    public record BedView(int number, boolean occupied, Long admissionId, Long patientId,
                          String patientName, String doctorName, boolean mine) {}

    public record WardOverview(Long id, String name, WardType wardType, int capacity, BigDecimal dailyRate,
                               String description, boolean active, int occupied, int available,
                               List<BedView> beds) {}

    // ---------- medical records ----------
    public record PatientIndexRow(Long id, String fullName, String gender, LocalDate dateOfBirth,
                                  String bloodGroup, String phoneNumber, int recordCount,
                                  boolean admitted, String wardName) {}

    public record DoctorEncounter(Long doctorId, String name, String doctorType, String specialization,
                                  int encounters, LocalDate lastSeen, List<String> through) {}

    public record PatientRecordSummary(Patient patient, List<MedicalRecord> records,
                                       List<DoctorEncounter> doctors, List<Admission> admissions,
                                       List<Administration> administrations, int appointmentCount) {}

    // ---------- patient "care team" ----------
    public record CareTeamMember(Long doctorId, String name, String doctorType, String specialization,
                                 String phoneNumber, int pendingAppointments, int pastAppointments,
                                 LocalDate nextAppointment, String nextAppointmentType) {}

    // ---------- receipts ----------
    public record Receipt(
            String hospitalName, String hospitalAddress, String hospitalPhone,
            String receiptNumber, String reference, String transactionRef,
            LocalDateTime paidAt, BigDecimal amount, String currency, String description, PaymentStatus status,
            String patientName, String patientNumber, String patientPhone,
            String payerName, String payerEmail, PaymentMethod method,
            String bankName, String accountName, String accountNumber, String cardBrand, String cardLast4,
            String receivedBy, String appointmentSummary) {}

    // ---------- dashboards ----------
    public record ReportSummary(long totalPatients, long totalDoctors, long totalNurses,
                                long appointmentsToday, long pendingAppointments, long totalAppointments,
                                long admittedPatients, long totalBeds, long lowStockItems,
                                long pendingPayments, BigDecimal totalRevenue) {}

    public record DoctorSummary(long appointmentsToday, long pendingAppointments, long admittedPatients) {}
}

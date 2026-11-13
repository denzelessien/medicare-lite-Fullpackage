package com.example.MEDICARE.dto;

import com.example.MEDICARE.model.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** All request bodies the API accepts. Using explicit DTOs (instead of raw entities) means callers can only set the fields we allow. */
public final class Requests {
    private Requests() {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record PatientRegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
            LocalDate dateOfBirth, String gender, String phoneNumber, String address, String bloodGroup) {}

    public record VerifyEmailRequest(@NotBlank String email, @NotBlank String code) {}

    public record ResendCodeRequest(@NotBlank String email) {}

    public record CreateStaffRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
            Role role, DoctorType doctorType, String specialization, String phoneNumber) {}

    public record EnabledRequest(boolean enabled) {}

    public record NurseRequest(@NotBlank String fullName, String email, String department, String phoneNumber) {}

    public record PatientRequest(
            @NotBlank String fullName, LocalDate dateOfBirth, String gender,
            String phoneNumber, String address, String bloodGroup) {}

    public record AppointmentRequest(
            Long patientId, Long doctorId, AppointmentType appointmentType,
            LocalDate appointmentDate, LocalTime appointmentTime, String reasonForVisit, Long admissionId) {}

    public record AppointmentStatusRequest(AppointmentStatus status) {}

    public record MedicalRecordRequest(
            Long patientId, Long doctorId, Long appointmentId, Long admissionId,
            String diagnosis, String prescription, String notes, LocalDate visitDate) {}

    public record CategoryRequest(@NotBlank String name, String description) {}

    public record ItemRequest(
            @NotBlank String name, ItemType itemType, Long categoryId, String unit,
            Integer stockQuantity, Integer reorderLevel, BigDecimal unitPrice,
            LocalDate expiryDate, String supplier) {}

    public record RestockRequest(Integer quantity) {}

    public record AdministerRequest(
            Long itemId, Long patientId, Integer quantity,
            Long appointmentId, Long admissionId, String notes, Boolean billPatient) {}

    public record WardRequest(
            @NotBlank String name, WardType wardType, Integer capacity,
            BigDecimal dailyRate, String description, Boolean active) {}

    public record AdmitRequest(Long patientId, Long wardId, Long doctorId, Integer bedNumber, String reason) {}

    public record DischargeRequest(String summary) {}

    public record ChargeRequest(Long patientId, Long appointmentId, BigDecimal amount, String description) {}

    public record PayRequest(
            PaymentMethod method,
            String cardNumber, String cardHolder, String expiry, String cvv,
            String bankName, String accountNumber, String accountName,
            String payerEmail) {}
}

package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Views.DoctorSummary;
import com.example.MEDICARE.dto.Views.ReportSummary;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.AppointmentService;
import com.example.MEDICARE.service.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private NurseRepository nurseRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private WardRepository wardRepository;
    @Autowired private InventoryItemRepository itemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AppointmentService appointmentService;
    @Autowired private CurrentUser currentUser;

    @Value("${app.timezone:Africa/Lagos}") private String timezone;

    @GetMapping("/summary")
    public ReportSummary summary() {
        appointmentService.autoCompleteDue();
        LocalDate today = LocalDate.now(ZoneId.of(timezone));

        long totalBeds = wardRepository.findAll().stream().filter(Ward::isActive).mapToLong(Ward::getCapacity).sum();
        long lowStock = itemRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(i -> i.getStockQuantity() != null && i.getStockQuantity() <= (i.getReorderLevel() == null ? 10 : i.getReorderLevel()))
                .count();
        BigDecimal revenue = paymentRepository.findByStatus(PaymentStatus.PAID).stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportSummary(
                patientRepository.count(), doctorRepository.count(), nurseRepository.count(),
                appointmentRepository.countByAppointmentDate(today),
                appointmentRepository.countByStatus(AppointmentStatus.PENDING),
                appointmentRepository.count(),
                admissionRepository.countByStatus(AdmissionStatus.ADMITTED), totalBeds, lowStock,
                paymentRepository.countByStatus(PaymentStatus.PENDING), revenue);
    }

    @GetMapping("/doctor")
    public DoctorSummary doctorSummary() {
        appointmentService.autoCompleteDue();
        Long did = currentUser.doctor().getId();
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        return new DoctorSummary(
                appointmentRepository.countByDoctorIdAndAppointmentDate(did, today),
                appointmentRepository.countByDoctorIdAndStatus(did, AppointmentStatus.PENDING),
                admissionRepository.findByAttendingDoctorIdAndStatusOrderByAdmittedAtDesc(did, AdmissionStatus.ADMITTED).size());
    }
}

package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.ChargeRequest;
import com.example.MEDICARE.dto.Requests.PayRequest;
import com.example.MEDICARE.dto.Views.Receipt;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.DemoPaymentGateway;
import com.example.MEDICARE.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private CurrentUser currentUser;

    /** Loads a payment and makes sure a patient can only touch their own. */
    private Payment load(Long id) {
        Payment p = paymentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Payment not found."));
        if (currentUser.isPatient() && !p.getPatient().getId().equals(currentUser.patient().getId())) {
            throw ApiException.notFound("Payment not found."); // don't reveal that it exists
        }
        return p;
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) throw ApiException.forbidden("Only hospital admins can do that.");
    }

    @GetMapping
    public List<Payment> list() {
        if (currentUser.isPatient()) return paymentRepository.findByPatientIdOrderByCreatedAtDesc(currentUser.patient().getId());
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/my")
    public List<Payment> mine() {
        return paymentRepository.findByPatientIdOrderByCreatedAtDesc(currentUser.patient().getId());
    }

    /** Banks offered by the demo checkout. */
    @GetMapping("/banks")
    public List<String> banks() { return DemoPaymentGateway.BANKS; }

    /** Admin raises a manual charge (consultation fee, deposit, anything). */
    @PostMapping
    public Payment createCharge(@RequestBody ChargeRequest req) {
        requireAdmin();
        if (req.patientId() == null) throw ApiException.badRequest("Choose a patient.");
        Patient patient = patientRepository.findById(req.patientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        Appointment appt = req.appointmentId() == null ? null
                : appointmentRepository.findById(req.appointmentId()).orElseThrow(() -> ApiException.notFound("Appointment not found."));
        return paymentService.createCharge(patient, appt, null, req.amount(),
                req.description() == null || req.description().isBlank() ? "Hospital charge" : req.description());
    }

    /** Pay a pending charge by card or bank transfer (demo gateway). Patients pay their own; admins can pay on a patient's behalf. */
    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable Long id, @RequestBody PayRequest req) {
        Payment p = load(id);
        var outcome = paymentService.pay(p, req, currentUser.email());
        if (!outcome.success()) {
            return ResponseEntity.status(402).body(Map.of("message", outcome.message()));
        }
        return ResponseEntity.ok(outcome.payment());
    }

    @PostMapping("/{id}/cash")
    public Payment cash(@PathVariable Long id) {
        requireAdmin();
        return paymentService.markCash(load(id), currentUser.user().getFullName(), null);
    }

    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable Long id) {
        requireAdmin();
        return paymentService.refund(load(id));
    }

    @PostMapping("/{id}/void")
    public Payment voidPayment(@PathVariable Long id) {
        requireAdmin();
        return paymentService.voidPayment(load(id));
    }

    @GetMapping("/{id}/receipt")
    public Receipt receipt(@PathVariable Long id) {
        return paymentService.receipt(load(id));
    }
}

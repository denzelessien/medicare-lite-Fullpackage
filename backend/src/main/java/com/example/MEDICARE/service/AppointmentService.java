package com.example.MEDICARE.service;

import com.example.MEDICARE.dto.Requests.AppointmentRequest;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private CurrentUser currentUser;

    @Value("${app.timezone:Africa/Lagos}") private String timezone;
    @Value("${app.appointments.auto-complete-grace-minutes:0}") private long graceMinutes;

    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of(timezone)); }

    private static LocalDateTime when(Appointment a) {
        return LocalDateTime.of(a.getAppointmentDate(), a.getAppointmentTime() != null ? a.getAppointmentTime() : LocalTime.MIDNIGHT);
    }

    // ------------------------------------------------------------------
    // Auto-complete: PENDING appointments whose time has passed -> COMPLETED
    // ------------------------------------------------------------------

    @Transactional
    public int autoCompleteDue() {
        LocalDateTime cutoff = now().minusMinutes(graceMinutes);
        List<Appointment> due = appointmentRepository
                .findByStatusAndAppointmentDateLessThanEqual(AppointmentStatus.PENDING, cutoff.toLocalDate());
        List<Appointment> changed = new ArrayList<>();
        for (Appointment a : due) {
            if (a.getAppointmentDate() != null && !when(a).isAfter(cutoff)) {
                a.setStatus(AppointmentStatus.COMPLETED);
                changed.add(a);
            }
        }
        if (!changed.isEmpty()) {
            appointmentRepository.saveAll(changed);
            log.info("Auto-completed {} appointment(s)", changed.size());
        }
        return changed.size();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void scheduledAutoComplete() {
        try { autoCompleteDue(); } catch (Exception e) { log.error("Auto-complete failed", e); }
    }

    // ------------------------------------------------------------------
    // Listing (scoped by role)
    // ------------------------------------------------------------------

    @Transactional
    public List<Appointment> list(Long doctorId, LocalDate date, AppointmentStatus status, AppointmentType type) {
        autoCompleteDue(); // keep statuses fresh even between scheduler ticks

        List<Appointment> base;
        if (currentUser.isPatient()) {
            base = appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(currentUser.patient().getId());
        } else if (currentUser.isDoctor()) {
            base = appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(currentUser.doctor().getId());
        } else if (doctorId != null) {
            base = appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(doctorId);
        } else {
            base = appointmentRepository.findAllByOrderByAppointmentDateDescAppointmentTimeDesc();
        }
        return base.stream()
                .filter(a -> date == null || date.equals(a.getAppointmentDate()))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> type == null || a.getAppointmentType() == type)
                .toList();
    }

    // ------------------------------------------------------------------
    // Booking
    // ------------------------------------------------------------------

    @Transactional
    public Appointment create(AppointmentRequest req) {
        Patient patient;
        Doctor doctor;

        if (currentUser.isPatient()) {
            patient = currentUser.patient();           // patients can only ever book for themselves
            doctor = findDoctor(req.doctorId());
        } else if (currentUser.isDoctor()) {
            doctor = currentUser.doctor();             // doctors can only book for themselves
            patient = findPatient(req.patientId());
        } else {
            patient = findPatient(req.patientId());
            doctor = findDoctor(req.doctorId());
        }

        if (req.appointmentDate() == null || req.appointmentTime() == null) {
            throw ApiException.badRequest("Choose a date and time.");
        }
        LocalDateTime at = LocalDateTime.of(req.appointmentDate(), req.appointmentTime());
        if (at.isBefore(now())) throw ApiException.badRequest("That time has already passed. Pick a future time.");

        AppointmentType type = req.appointmentType() != null ? req.appointmentType() : AppointmentType.CHECKUP;
        var allowed = type.allowedDoctorTypes();
        if (!allowed.isEmpty() && !allowed.contains(doctor.getDoctorType())) {
            throw ApiException.badRequest(type.getLabel() + " must be booked with a "
                    + String.join(" or ", allowed.stream().map(DoctorType::getLabel).toList()) + ".");
        }

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatus(
                doctor.getId(), req.appointmentDate(), req.appointmentTime(), AppointmentStatus.PENDING)) {
            throw ApiException.conflict("That doctor already has an appointment at that time. Choose another slot.");
        }

        Admission admission = null;
        if (req.admissionId() != null) {
            admission = admissionRepository.findById(req.admissionId())
                    .orElseThrow(() -> ApiException.notFound("Admission not found."));
            if (!admission.getPatient().getId().equals(patient.getId()) || admission.getStatus() != AdmissionStatus.ADMITTED) {
                throw ApiException.badRequest("That admission doesn't belong to this patient or is no longer active.");
            }
        } else if (type == AppointmentType.SURGERY) {
            // Surgery for a patient who is currently on a ward: attach it to their stay automatically.
            admission = admissionRepository.findFirstByPatientIdAndStatus(patient.getId(), AdmissionStatus.ADMITTED).orElse(null);
        }

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setAppointmentType(type);
        a.setAppointmentDate(req.appointmentDate());
        a.setAppointmentTime(req.appointmentTime());
        a.setStatus(AppointmentStatus.PENDING);
        a.setReasonForVisit(req.reasonForVisit());
        a.setAdmission(admission);
        a.setCreatedBy(currentUser.user());
        a = appointmentRepository.save(a);

        // Every appointment raises a fee the patient can settle from "My payments".
        paymentService.createCharge(patient, a, admission, type.getDefaultFee(),
                type.getLabel() + " fee - " + a.getAppointmentDate());
        return a;
    }

    private Patient findPatient(Long id) {
        if (id == null) throw ApiException.badRequest("Choose a patient.");
        return patientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found."));
    }

    private Doctor findDoctor(Long id) {
        if (id == null) throw ApiException.badRequest("Choose a doctor.");
        Doctor d = doctorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        if (!d.getUser().isEnabled()) throw ApiException.badRequest("That doctor is not available.");
        return d;
    }

    // ------------------------------------------------------------------
    // Status changes
    // ------------------------------------------------------------------

    /** Admin (any appointment) or doctor (their own) sets the status by hand. */
    @Transactional
    public Appointment updateStatus(Long id, AppointmentStatus status) {
        if (status == null) throw ApiException.badRequest("Choose a status.");
        Appointment a = appointmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found."));

        if (currentUser.isDoctor() && !a.getDoctor().getId().equals(currentUser.doctor().getId())) {
            throw ApiException.forbidden("You can only update your own appointments.");
        }
        if (a.getStatus() == AppointmentStatus.CANCELLED && status != AppointmentStatus.CANCELLED) {
            throw ApiException.badRequest("A cancelled appointment can't be reopened. Book a new one instead.");
        }
        if (status == AppointmentStatus.PENDING && !when(a).isAfter(now())) {
            throw ApiException.badRequest("This appointment's time has already passed, so it can't go back to pending.");
        }

        a.setStatus(status);
        if (status == AppointmentStatus.CANCELLED) paymentService.voidPendingForAppointment(a.getId());
        return appointmentRepository.save(a);
    }

    /** A patient cancels one of their own upcoming appointments. */
    @Transactional
    public Appointment cancelOwn(Long id) {
        Appointment a = appointmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found."));
        if (!a.getPatient().getId().equals(currentUser.patient().getId())) {
            throw ApiException.forbidden("That isn't your appointment.");
        }
        if (a.getStatus() != AppointmentStatus.PENDING) {
            throw ApiException.badRequest("Only pending appointments can be cancelled.");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        paymentService.voidPendingForAppointment(a.getId());
        return appointmentRepository.save(a);
    }
}

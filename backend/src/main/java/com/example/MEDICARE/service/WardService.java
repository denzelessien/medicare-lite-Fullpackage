package com.example.MEDICARE.service;

import com.example.MEDICARE.dto.Requests.AdmitRequest;
import com.example.MEDICARE.dto.Views.BedView;
import com.example.MEDICARE.dto.Views.WardOverview;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class WardService {

    @Autowired private WardRepository wardRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private AccessService accessService;
    @Autowired private CurrentUser currentUser;

    @Value("${app.timezone:Africa/Lagos}") private String timezone;

    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of(timezone)); }

    // ---------------- ward overview with bed map ----------------

    public WardOverview overview(Ward w) {
        Long myDoctorId = currentUser.isDoctor() ? currentUser.doctor().getId() : null;
        Map<Integer, Admission> byBed = new HashMap<>();
        for (Admission a : admissionRepository.findByWardIdAndStatus(w.getId(), AdmissionStatus.ADMITTED)) {
            if (a.getBedNumber() != null) byBed.put(a.getBedNumber(), a);
        }
        List<BedView> beds = new ArrayList<>();
        for (int i = 1; i <= w.getCapacity(); i++) {
            Admission a = byBed.get(i);
            if (a == null) {
                beds.add(new BedView(i, false, null, null, null, null, false));
            } else {
                boolean mine = myDoctorId == null || (a.getAttendingDoctor() != null && myDoctorId.equals(a.getAttendingDoctor().getId()));
                // A doctor only sees the names of patients under their own care.
                beds.add(new BedView(i, true, a.getId(), mine ? a.getPatient().getId() : null,
                        mine ? a.getPatient().getFullName() : null,
                        a.getAttendingDoctor() != null ? a.getAttendingDoctor().getUser().getFullName() : null, mine));
            }
        }
        int occupied = (int) beds.stream().filter(BedView::occupied).count();
        return new WardOverview(w.getId(), w.getName(), w.getWardType(), w.getCapacity(), w.getDailyRate(),
                w.getDescription(), w.isActive(), occupied, Math.max(0, w.getCapacity() - occupied), beds);
    }

    // ---------------- admit ----------------

    @Transactional
    public Admission admit(AdmitRequest req) {
        if (req.patientId() == null) throw ApiException.badRequest("Choose a patient.");
        if (req.wardId() == null) throw ApiException.badRequest("Choose a ward.");

        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> ApiException.notFound("Patient not found."));
        accessService.requirePatientAccess(patient.getId()); // doctors can only admit patients they treat
        if (admissionRepository.findFirstByPatientIdAndStatus(patient.getId(), AdmissionStatus.ADMITTED).isPresent()) {
            throw ApiException.conflict(patient.getFullName() + " is already admitted. Discharge them first.");
        }

        Doctor doctor;
        if (currentUser.isDoctor()) {
            doctor = currentUser.doctor();
        } else {
            if (req.doctorId() == null) throw ApiException.badRequest("Choose the attending doctor.");
            doctor = doctorRepository.findById(req.doctorId()).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        }

        // Lock the ward row so two simultaneous admissions can't take the same bed.
        Ward ward = wardRepository.findByIdForUpdate(req.wardId()).orElseThrow(() -> ApiException.notFound("Ward not found."));
        if (!ward.isActive()) throw ApiException.badRequest(ward.getName() + " is closed to new admissions.");

        Set<Integer> taken = new HashSet<>();
        for (Admission a : admissionRepository.findByWardIdAndStatus(ward.getId(), AdmissionStatus.ADMITTED)) {
            if (a.getBedNumber() != null) taken.add(a.getBedNumber());
        }
        if (taken.size() >= ward.getCapacity()) throw ApiException.conflict(ward.getName() + " is full.");

        int bed;
        if (req.bedNumber() != null) {
            bed = req.bedNumber();
            if (bed < 1 || bed > ward.getCapacity()) throw ApiException.badRequest("Bed number must be between 1 and " + ward.getCapacity() + ".");
            if (taken.contains(bed)) throw ApiException.conflict("Bed " + bed + " is already occupied.");
        } else {
            bed = 1;
            while (taken.contains(bed)) bed++;
        }

        Admission a = new Admission();
        a.setPatient(patient);
        a.setWard(ward);
        a.setBedNumber(bed);
        a.setAttendingDoctor(doctor);
        a.setReasonForAdmission(req.reason());
        a.setStatus(AdmissionStatus.ADMITTED);
        a.setAdmittedAt(now());
        a.setAdmittedBy(currentUser.user().getFullName());
        return admissionRepository.save(a);
    }

    // ---------------- discharge ----------------

    @Transactional
    public Admission discharge(Long id, String summary) {
        Admission a = admissionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Admission not found."));
        if (a.getStatus() != AdmissionStatus.ADMITTED) throw ApiException.badRequest("This patient has already been discharged.");
        if (currentUser.isDoctor()
                && (a.getAttendingDoctor() == null || !a.getAttendingDoctor().getId().equals(currentUser.doctor().getId()))) {
            throw ApiException.forbidden("Only the attending doctor can discharge this patient.");
        }

        a.setStatus(AdmissionStatus.DISCHARGED);
        a.setDischargedAt(now());
        a.setDischargeSummary(summary);
        a = admissionRepository.save(a);

        // Bill the stay: whole days (minimum 1) x the ward's daily rate.
        BigDecimal rate = a.getWard().getDailyRate();
        if (rate != null && rate.signum() > 0) {
            long hours = Duration.between(a.getAdmittedAt(), a.getDischargedAt()).toHours();
            long days = Math.max(1, (long) Math.ceil(hours / 24.0));
            paymentService.createCharge(a.getPatient(), null, a, rate.multiply(BigDecimal.valueOf(days)),
                    "Ward stay: " + a.getWard().getName() + " (" + days + (days == 1 ? " day)" : " days)"));
        }
        return a;
    }
}

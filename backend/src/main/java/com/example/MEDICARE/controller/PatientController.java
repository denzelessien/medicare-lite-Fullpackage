package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.PatientRequest;
import com.example.MEDICARE.dto.Views.CareTeamMember;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.AccessService;
import com.example.MEDICARE.service.AppointmentService;
import com.example.MEDICARE.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private MedicalRecordRepository recordRepository;
    @Autowired private AppointmentService appointmentService;
    @Autowired private AccessService accessService;
    @Autowired private CurrentUser currentUser;

    @Value("${app.timezone:Africa/Lagos}") private String timezone;

    /** Admins see every patient; doctors only the patients they treat. */
    @GetMapping
    public List<Patient> list() {
        List<Patient> all = patientRepository.findAll();
        if (currentUser.isDoctor()) {
            Long did = currentUser.doctor().getId();
            return all.stream().filter(p -> accessService.doctorTreats(did, p.getId())).toList();
        }
        return all;
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable Long id) {
        accessService.requirePatientAccess(id);
        return patientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found."));
    }

    @PostMapping
    public Patient create(@Valid @RequestBody PatientRequest req) {
        if (!currentUser.isAdmin()) throw ApiException.forbidden("Only admins can register patients.");
        Patient p = new Patient();
        apply(p, req);
        return patientRepository.save(p);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) {
        if (!currentUser.isAdmin()) throw ApiException.forbidden("Only admins can edit patients.");
        Patient p = patientRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found."));
        apply(p, req);
        return patientRepository.save(p);
    }

    private void apply(Patient p, PatientRequest r) {
        p.setFullName(r.fullName().trim());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setPhoneNumber(r.phoneNumber());
        p.setAddress(r.address());
        p.setBloodGroup(r.bloodGroup());
    }

    // ---------------- patient self-service ----------------

    @GetMapping("/me")
    public Patient me() { return currentUser.patient(); }

    /** The doctors who are treating (or will treat) the signed-in patient. */
    @GetMapping("/me/care-team")
    public List<CareTeamMember> careTeam() {
        appointmentService.autoCompleteDue();
        Patient me = currentUser.patient();
        LocalDate today = LocalDate.now(ZoneId.of(timezone));

        Map<Long, Doctor> doctors = new LinkedHashMap<>();
        Map<Long, int[]> counts = new HashMap<>();            // [pending, past]
        Map<Long, Appointment> next = new HashMap<>();

        for (Appointment a : appointmentRepository.findByPatientId(me.getId())) {
            if (a.getStatus() == AppointmentStatus.CANCELLED) continue;
            Long id = a.getDoctor().getId();
            doctors.putIfAbsent(id, a.getDoctor());
            int[] c = counts.computeIfAbsent(id, k -> new int[2]);
            if (a.getStatus() == AppointmentStatus.PENDING) {
                c[0]++;
                Appointment n = next.get(id);
                if (!a.getAppointmentDate().isBefore(today)
                        && (n == null || a.getAppointmentDate().isBefore(n.getAppointmentDate()))) next.put(id, a);
            } else {
                c[1]++;
            }
        }
        for (Admission a : admissionRepository.findByPatientIdOrderByAdmittedAtDesc(me.getId())) {
            if (a.getAttendingDoctor() != null) {
                doctors.putIfAbsent(a.getAttendingDoctor().getId(), a.getAttendingDoctor());
                counts.computeIfAbsent(a.getAttendingDoctor().getId(), k -> new int[2]);
            }
        }
        for (MedicalRecord r : recordRepository.findByPatientIdOrderByVisitDateDescIdDesc(me.getId())) {
            doctors.putIfAbsent(r.getDoctor().getId(), r.getDoctor());
            counts.computeIfAbsent(r.getDoctor().getId(), k -> new int[2]);
        }

        return doctors.values().stream().map(d -> {
            int[] c = counts.get(d.getId());
            Appointment n = next.get(d.getId());
            return new CareTeamMember(d.getId(), d.getUser().getFullName(), d.getDoctorType().getLabel(),
                    d.getSpecialization(), d.getPhoneNumber(), c[0], c[1],
                    n != null ? n.getAppointmentDate() : null,
                    n != null ? n.getAppointmentType().getLabel() : null);
        }).toList();
    }
}

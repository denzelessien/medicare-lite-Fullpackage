package com.example.MEDICARE.service;

import com.example.MEDICARE.dto.Views.*;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** Builds the "full picture" of a patient: records, treating doctors, ward stays and medication given. */
@Service
public class RecordsService {

    @Autowired private PatientRepository patientRepository;
    @Autowired private MedicalRecordRepository recordRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private AdministrationRepository administrationRepository;
    @Autowired private AccessService accessService;
    @Autowired private CurrentUser currentUser;

    /** Patients the caller may open: everyone for admins, only their own patients for doctors. */
    public List<PatientIndexRow> index() {
        List<Patient> patients = patientRepository.findAll();
        if (currentUser.isDoctor()) {
            Long did = currentUser.doctor().getId();
            patients = patients.stream().filter(p -> accessService.doctorTreats(did, p.getId())).toList();
        }

        Map<Long, Integer> recordCounts = new HashMap<>();
        for (MedicalRecord r : recordRepository.findAll()) recordCounts.merge(r.getPatient().getId(), 1, Integer::sum);
        Map<Long, String> wardByPatient = new HashMap<>();
        for (Admission a : admissionRepository.findByStatusOrderByAdmittedAtDesc(AdmissionStatus.ADMITTED)) {
            wardByPatient.put(a.getPatient().getId(), a.getWard().getName());
        }

        return patients.stream()
                .sorted(Comparator.comparing(p -> p.getFullName().toLowerCase()))
                .map(p -> new PatientIndexRow(p.getId(), p.getFullName(), p.getGender(), p.getDateOfBirth(),
                        p.getBloodGroup(), p.getPhoneNumber(), recordCounts.getOrDefault(p.getId(), 0),
                        wardByPatient.containsKey(p.getId()), wardByPatient.get(p.getId())))
                .toList();
    }

    public PatientRecordSummary summary(Long patientId) {
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found."));
        accessService.requirePatientAccess(patientId);

        List<MedicalRecord> records = recordRepository.findByPatientIdOrderByVisitDateDescIdDesc(patientId);
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        List<Admission> admissions = admissionRepository.findByPatientIdOrderByAdmittedAtDesc(patientId);
        List<Administration> administrations = administrationRepository.findByPatientIdOrderByAdministeredAtDesc(patientId);

        // ---- which doctors have treated this patient, and through what ----
        Map<Long, Doctor> doctors = new LinkedHashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, LocalDate> last = new HashMap<>();
        Map<Long, Set<String>> through = new HashMap<>();

        for (MedicalRecord r : records) touch(doctors, counts, last, through, r.getDoctor(), r.getVisitDate(), "Medical records");
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.COMPLETED) {
                touch(doctors, counts, last, through, a.getDoctor(), a.getAppointmentDate(), "Appointments");
            }
        }
        for (Admission a : admissions) {
            if (a.getAttendingDoctor() != null) {
                touch(doctors, counts, last, through, a.getAttendingDoctor(), a.getAdmittedAt().toLocalDate(), "Ward care");
            }
        }

        List<DoctorEncounter> team = doctors.values().stream().map(d -> new DoctorEncounter(
                d.getId(), d.getUser().getFullName(), d.getDoctorType().getLabel(), d.getSpecialization(),
                counts.get(d.getId()), last.get(d.getId()), new ArrayList<>(through.get(d.getId()))))
                .sorted(Comparator.comparing(DoctorEncounter::lastSeen, Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();

        return new PatientRecordSummary(patient, records, team, admissions, administrations, appointments.size());
    }

    private void touch(Map<Long, Doctor> doctors, Map<Long, Integer> counts, Map<Long, LocalDate> last,
                       Map<Long, Set<String>> through, Doctor d, LocalDate date, String via) {
        if (d == null) return;
        doctors.putIfAbsent(d.getId(), d);
        counts.merge(d.getId(), 1, Integer::sum);
        through.computeIfAbsent(d.getId(), k -> new LinkedHashSet<>()).add(via);
        if (date != null && (last.get(d.getId()) == null || date.isAfter(last.get(d.getId())))) last.put(d.getId(), date);
    }
}

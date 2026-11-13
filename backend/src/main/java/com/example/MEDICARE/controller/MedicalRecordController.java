package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.MedicalRecordRequest;
import com.example.MEDICARE.dto.Views.PatientIndexRow;
import com.example.MEDICARE.dto.Views.PatientRecordSummary;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.AccessService;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.RecordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    @Autowired private MedicalRecordRepository recordRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private RecordsService recordsService;
    @Autowired private AccessService accessService;
    @Autowired private CurrentUser currentUser;

    /** The list on the left of the Medical Records page. */
    @GetMapping("/patients")
    public List<PatientIndexRow> index() { return recordsService.index(); }

    /** Everything about one patient: records, doctors who treated them, ward stays, medication given. */
    @GetMapping("/patient/{patientId}/summary")
    public PatientRecordSummary summary(@PathVariable Long patientId) { return recordsService.summary(patientId); }

    @GetMapping("/patient/{patientId}")
    public List<MedicalRecord> byPatient(@PathVariable Long patientId) {
        accessService.requirePatientAccess(patientId);
        return recordRepository.findByPatientIdOrderByVisitDateDescIdDesc(patientId);
    }

    @PostMapping
    public MedicalRecord create(@RequestBody MedicalRecordRequest req) {
        if (req.patientId() == null) throw ApiException.badRequest("Choose a patient.");
        Patient patient = patientRepository.findById(req.patientId()).orElseThrow(() -> ApiException.notFound("Patient not found."));
        accessService.requirePatientAccess(patient.getId());

        Doctor doctor;
        if (currentUser.isDoctor()) {
            doctor = currentUser.doctor();
        } else {
            if (req.doctorId() == null) throw ApiException.badRequest("Choose the treating doctor.");
            doctor = doctorRepository.findById(req.doctorId()).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        }
        if (req.visitDate() == null) throw ApiException.badRequest("Enter the visit date.");
        if ((req.diagnosis() == null || req.diagnosis().isBlank()) && (req.notes() == null || req.notes().isBlank())
                && (req.prescription() == null || req.prescription().isBlank())) {
            throw ApiException.badRequest("Add a diagnosis, prescription or notes.");
        }

        MedicalRecord r = new MedicalRecord();
        r.setPatient(patient);
        r.setDoctor(doctor);
        r.setDiagnosis(req.diagnosis());
        r.setPrescription(req.prescription());
        r.setNotes(req.notes());
        r.setVisitDate(req.visitDate());

        if (req.appointmentId() != null) {
            Appointment a = appointmentRepository.findById(req.appointmentId()).orElseThrow(() -> ApiException.notFound("Appointment not found."));
            if (!a.getPatient().getId().equals(patient.getId())) throw ApiException.badRequest("That appointment belongs to a different patient.");
            r.setAppointment(a);
        }
        if (req.admissionId() != null) {
            Admission a = admissionRepository.findById(req.admissionId()).orElseThrow(() -> ApiException.notFound("Admission not found."));
            if (!a.getPatient().getId().equals(patient.getId())) throw ApiException.badRequest("That admission belongs to a different patient.");
            r.setAdmission(a);
        }
        return recordRepository.save(r);
    }
}

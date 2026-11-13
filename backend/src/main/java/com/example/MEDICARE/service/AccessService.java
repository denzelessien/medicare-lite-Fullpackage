package com.example.MEDICARE.service;

import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Rules about which patients a doctor is allowed to see. Admins see everyone. */
@Service
public class AccessService {

    @Autowired private CurrentUser currentUser;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private MedicalRecordRepository medicalRecordRepository;

    /** A doctor "treats" a patient if they share an appointment, an admission, or a medical record. */
    public boolean doctorTreats(Long doctorId, Long patientId) {
        return appointmentRepository.existsByDoctorIdAndPatientId(doctorId, patientId)
                || admissionRepository.existsByAttendingDoctorIdAndPatientId(doctorId, patientId)
                || medicalRecordRepository.existsByDoctorIdAndPatientId(doctorId, patientId);
    }

    /** Throws 403 unless the caller is an admin or a doctor who treats this patient. */
    public void requirePatientAccess(Long patientId) {
        if (currentUser.isAdmin()) return;
        if (currentUser.isDoctor() && doctorTreats(currentUser.doctor().getId(), patientId)) return;
        throw ApiException.forbidden("You can only access patients you are treating.");
    }
}

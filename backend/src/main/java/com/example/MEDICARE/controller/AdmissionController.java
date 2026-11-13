package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.AdmitRequest;
import com.example.MEDICARE.dto.Requests.DischargeRequest;
import com.example.MEDICARE.model.Admission;
import com.example.MEDICARE.model.AdmissionStatus;
import com.example.MEDICARE.repository.AdmissionRepository;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.WardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionController {

    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private WardService wardService;
    @Autowired private CurrentUser currentUser;

    /** Admins: all admissions. Doctors: only the patients they are the attending doctor for. */
    @GetMapping
    public List<Admission> list(@RequestParam(required = false) AdmissionStatus status) {
        if (currentUser.isDoctor()) {
            Long did = currentUser.doctor().getId();
            return status != null
                    ? admissionRepository.findByAttendingDoctorIdAndStatusOrderByAdmittedAtDesc(did, status)
                    : admissionRepository.findByAttendingDoctorIdOrderByAdmittedAtDesc(did);
        }
        return status != null
                ? admissionRepository.findByStatusOrderByAdmittedAtDesc(status)
                : admissionRepository.findAllByOrderByAdmittedAtDesc();
    }

    @PostMapping
    public Admission admit(@RequestBody AdmitRequest req) { return wardService.admit(req); }

    @PostMapping("/{id}/discharge")
    public Admission discharge(@PathVariable Long id, @RequestBody(required = false) DischargeRequest req) {
        return wardService.discharge(id, req != null ? req.summary() : null);
    }
}

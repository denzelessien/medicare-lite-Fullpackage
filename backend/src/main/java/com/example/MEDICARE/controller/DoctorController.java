package com.example.MEDICARE.controller;

import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.Doctor;
import com.example.MEDICARE.model.DoctorType;
import com.example.MEDICARE.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/** Read-only doctor directory used by the booking forms. Creating doctors happens in /api/staff. */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired private DoctorRepository doctorRepository;

    public record TypeInfo(String value, String label) {}

    @GetMapping
    public List<Doctor> list(@RequestParam(required = false) DoctorType type) {
        return doctorRepository.findByUserEnabledTrue().stream()
                .filter(d -> type == null || d.getDoctorType() == type)
                .toList();
    }

    @GetMapping("/types")
    public List<TypeInfo> types() {
        return Arrays.stream(DoctorType.values()).map(t -> new TypeInfo(t.name(), t.getLabel())).toList();
    }

    @GetMapping("/{id}")
    public Doctor get(@PathVariable Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found."));
    }
}

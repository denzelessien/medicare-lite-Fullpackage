package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.*;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Admin-only: the login accounts for hospital staff (Admins and Doctors). */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CurrentUser currentUser;

    public record StaffRow(Long id, String fullName, String email, Role role, boolean enabled, LocalDateTime createdAt,
                           Long doctorId, DoctorType doctorType, String doctorTypeLabel,
                           String specialization, String phoneNumber) {}

    private StaffRow row(User u) {
        Doctor d = u.getRole() == Role.DOCTOR ? doctorRepository.findByUserId(u.getId()).orElse(null) : null;
        return new StaffRow(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt(),
                d != null ? d.getId() : null,
                d != null ? d.getDoctorType() : null,
                d != null ? d.getDoctorType().getLabel() : null,
                d != null ? d.getSpecialization() : null,
                d != null ? d.getPhoneNumber() : null);
    }

    @GetMapping
    public List<StaffRow> list() {
        return userRepository.findByRoleNotOrderByFullNameAsc(Role.PATIENT).stream().map(this::row).toList();
    }

    /**
     * Creates an Admin or Doctor account. (The old version took a raw User body, and because the password
     * field is hidden from JSON the password always arrived empty - that's why creating accounts failed.)
     */
    @PostMapping
    @Transactional
    public StaffRow create(@Valid @RequestBody CreateStaffRequest req) {
        if (req.role() != Role.ADMIN && req.role() != Role.DOCTOR) {
            throw ApiException.badRequest("Staff accounts can be Admin or Doctor.");
        }
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw ApiException.conflict("That email is already registered.");

        User user = new User(req.fullName().trim(), email, passwordEncoder.encode(req.password()), req.role());
        user.setEmailVerified(true); // created by an admin, so no self-verification needed
        userRepository.save(user);

        if (req.role() == Role.DOCTOR) {
            Doctor doctor = new Doctor();
            doctor.setUser(user);
            doctor.setDoctorType(req.doctorType() != null ? req.doctorType() : DoctorType.GENERAL_PRACTITIONER);
            doctor.setSpecialization(req.specialization());
            doctor.setPhoneNumber(req.phoneNumber());
            doctorRepository.save(doctor);
        }
        return row(user);
    }

    @PutMapping("/{id}/enabled")
    public StaffRow setEnabled(@PathVariable Long id, @RequestBody EnabledRequest body) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Account not found."));
        if (user.getRole() == Role.PATIENT) throw ApiException.badRequest("Not a staff account.");
        if (user.getId().equals(currentUser.user().getId())) {
            throw ApiException.badRequest("You can't deactivate your own account.");
        }
        user.setEnabled(body.enabled());
        return row(userRepository.save(user));
    }
}

package com.example.MEDICARE.service;

import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Who is calling? Resolves the JWT-authenticated caller to their User / Doctor / Patient rows. */
@Service
public class CurrentUser {

    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;

    public String email() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in to continue.");
        return auth.getName();
    }

    public User user() {
        return userRepository.findByEmail(email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in to continue."));
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }
    public boolean isDoctor() { return hasRole("DOCTOR"); }
    public boolean isPatient() { return hasRole("PATIENT"); }

    public Doctor doctor() {
        return doctorRepository.findByUserId(user().getId())
                .orElseThrow(() -> ApiException.forbidden("No doctor profile is linked to this account."));
    }

    public Patient patient() {
        return patientRepository.findByUserEmail(email())
                .orElseThrow(() -> ApiException.badRequest("No patient profile is linked to this account."));
    }
}

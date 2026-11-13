package com.example.MEDICARE.controller;

import com.example.MEDICARE.config.JwtUtil;
import com.example.MEDICARE.dto.Requests.*;
import com.example.MEDICARE.dto.Views.AuthResponse;
import com.example.MEDICARE.dto.Views.MessageResponse;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private VerificationService verificationService;
    @Autowired private CurrentUser currentUser;

    private static String norm(String email) { return email == null ? "" : email.trim().toLowerCase(); }

    private AuthResponse build(User user) {
        Doctor doctor = user.getRole() == Role.DOCTOR ? doctorRepository.findByUserId(user.getId()).orElse(null) : null;
        Patient patient = user.getRole() == Role.PATIENT ? patientRepository.findByUserId(user.getId()).orElse(null) : null;
        return new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole().name()),
                user.getRole().name(), user.getFullName(), user.getEmail(), user.getId(),
                doctor != null ? doctor.getId() : null,
                patient != null ? patient.getId() : null,
                doctor != null ? doctor.getDoctorType().name() : null);
    }

    /** Step 1 of patient sign-up: create the (unverified) account and email a 6-digit code. */
    @PostMapping("/patient-register")
    @Transactional
    public MessageResponse patientRegister(@Valid @RequestBody PatientRegisterRequest req) {
        String email = norm(req.email());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.getRole() != Role.PATIENT || user.isEmailVerified()) {
                throw ApiException.conflict("That email is already registered. Try signing in instead.");
            }
            // Same person retrying before verifying: refresh their details and send a new code.
            user.setFullName(req.fullName().trim());
            user.setPassword(passwordEncoder.encode(req.password()));
            userRepository.save(user);
        } else {
            user = new User(req.fullName().trim(), email, passwordEncoder.encode(req.password()), Role.PATIENT);
            user.setEmailVerified(false);
            userRepository.save(user);
        }

        Patient patient = patientRepository.findByUserId(user.getId()).orElseGet(Patient::new);
        patient.setFullName(req.fullName().trim());
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setGender(req.gender());
        patient.setPhoneNumber(req.phoneNumber());
        patient.setAddress(req.address());
        patient.setBloodGroup(req.bloodGroup());
        patient.setUser(user);
        patientRepository.save(patient);

        boolean sent = verificationService.issueCode(user, true);
        return new MessageResponse("We sent a 6-digit code to " + email + ".", !sent);
    }

    /** Step 2: confirm the code. On success the patient is signed straight in. */
    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        User user = verificationService.verify(norm(req.email()), req.code());
        return build(user);
    }

    @PostMapping("/resend-code")
    public MessageResponse resend(@Valid @RequestBody ResendCodeRequest req) {
        User user = userRepository.findByEmail(norm(req.email())).orElse(null);
        boolean devMode = false;
        if (user != null && !user.isEmailVerified()) {
            devMode = !verificationService.issueCode(user, true);
        }
        // Same answer whether or not the email exists, so this can't be used to probe for accounts.
        return new MessageResponse("If that email is waiting for verification, a new code is on its way.", devMode);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(norm(req.email())).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account has been deactivated. Please contact the hospital admin.");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email address to continue.", "EMAIL_NOT_VERIFIED");
        }
        return build(user);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        return build(currentUser.user());
    }
}

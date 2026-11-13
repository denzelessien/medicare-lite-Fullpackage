package com.example.MEDICARE.service;

import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.EmailVerification;
import com.example.MEDICARE.model.User;
import com.example.MEDICARE.repository.EmailVerificationRepository;
import com.example.MEDICARE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/** Creates, emails and checks the 6-digit email verification codes. Codes are stored hashed. */
@Service
public class VerificationService {

    @Autowired private EmailVerificationRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Value("${app.verification.code-ttl-minutes:10}") private int ttlMinutes;
    @Value("${app.verification.resend-cooldown-seconds:60}") private int cooldownSeconds;
    @Value("${app.verification.max-attempts:5}") private int maxAttempts;

    private final SecureRandom random = new SecureRandom();

    /** Generates a fresh code (replacing any old one) and emails it. @return true if a real email went out. */
    @Transactional
    public boolean issueCode(User user, boolean enforceCooldown) {
        var latest = repository.findFirstByEmailOrderByCreatedAtDesc(user.getEmail());
        if (enforceCooldown && latest.isPresent()) {
            long since = Duration.between(latest.get().getCreatedAt(), LocalDateTime.now()).getSeconds();
            if (since < cooldownSeconds) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + (cooldownSeconds - since) + "s before asking for another code.");
            }
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        repository.deleteByEmail(user.getEmail());

        EmailVerification ev = new EmailVerification();
        ev.setEmail(user.getEmail());
        ev.setCodeHash(passwordEncoder.encode(code));
        ev.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        repository.save(ev);

        return emailService.sendVerificationCode(user.getEmail(), user.getFullName(), code);
    }

    /** noRollbackFor: a wrong guess must still be counted, even though we throw. */
    @Transactional(noRollbackFor = ApiException.class)
    public User verify(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired code."));
        if (user.isEmailVerified()) return user;

        EmailVerification ev = repository.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> ApiException.badRequest("No code was requested. Please request a new one."));

        if (ev.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("That code has expired. Please request a new one.");
        }
        if (ev.getAttempts() >= maxAttempts) {
            throw ApiException.badRequest("Too many wrong attempts. Please request a new code.");
        }
        if (!passwordEncoder.matches(code.trim(), ev.getCodeHash())) {
            ev.setAttempts(ev.getAttempts() + 1);
            repository.save(ev);
            throw ApiException.badRequest("That code isn't right. Check the email and try again.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        repository.deleteByEmail(email);
        return user;
    }
}

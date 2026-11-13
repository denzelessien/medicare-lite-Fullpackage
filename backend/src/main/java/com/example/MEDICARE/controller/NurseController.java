package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.EnabledRequest;
import com.example.MEDICARE.dto.Requests.NurseRequest;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.Nurse;
import com.example.MEDICARE.repository.NurseRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Nurse directory (admin only). Nurses are listed for reference - they don't have logins. */
@RestController
@RequestMapping("/api/nurses")
public class NurseController {

    @Autowired private NurseRepository nurseRepository;

    @GetMapping
    public List<Nurse> list() { return nurseRepository.findAll(); }

    @PostMapping
    public Nurse create(@Valid @RequestBody NurseRequest req) {
        Nurse n = new Nurse();
        n.setFullName(req.fullName().trim());
        n.setEmail(req.email());
        n.setDepartment(req.department());
        n.setPhoneNumber(req.phoneNumber());
        return nurseRepository.save(n);
    }

    @PutMapping("/{id}/active")
    public Nurse setActive(@PathVariable Long id, @RequestBody EnabledRequest body) {
        Nurse n = nurseRepository.findById(id).orElseThrow(() -> ApiException.notFound("Nurse not found."));
        n.setActive(body.enabled());
        return nurseRepository.save(n);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!nurseRepository.existsById(id)) throw ApiException.notFound("Nurse not found.");
        nurseRepository.deleteById(id);
    }
}

package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.WardRequest;
import com.example.MEDICARE.dto.Views.WardOverview;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.WardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wards")
public class WardController {

    @Autowired private WardRepository wardRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private WardService wardService;
    @Autowired private CurrentUser currentUser;

    @GetMapping
    public List<WardOverview> list() {
        return wardRepository.findAll().stream().map(wardService::overview).toList();
    }

    @GetMapping("/{id}")
    public WardOverview get(@PathVariable Long id) {
        return wardService.overview(wardRepository.findById(id).orElseThrow(() -> ApiException.notFound("Ward not found.")));
    }

    /** Pending surgeries for patients admitted to this ward (doctors only see their own). */
    @GetMapping("/{id}/surgeries")
    public List<Appointment> surgeries(@PathVariable Long id) {
        var list = appointmentRepository.findByAppointmentTypeAndStatusAndAdmissionWardIdOrderByAppointmentDateAscAppointmentTimeAsc(
                AppointmentType.SURGERY, AppointmentStatus.PENDING, id);
        if (currentUser.isDoctor()) {
            Long did = currentUser.doctor().getId();
            return list.stream().filter(a -> a.getDoctor().getId().equals(did)).toList();
        }
        return list;
    }

    private void apply(Ward w, WardRequest r) {
        w.setName(r.name().trim());
        w.setWardType(r.wardType() != null ? r.wardType() : WardType.GENERAL);
        int cap = r.capacity() != null ? r.capacity() : 10;
        if (cap < 1) throw ApiException.badRequest("A ward needs at least 1 bed.");
        if (w.getId() != null) {
            long occupied = admissionRepository.countByWardIdAndStatus(w.getId(), AdmissionStatus.ADMITTED);
            if (cap < occupied) throw ApiException.badRequest("Capacity can't be lower than the " + occupied + " patients currently admitted.");
        }
        w.setCapacity(cap);
        w.setDailyRate(r.dailyRate());
        w.setDescription(r.description());
        if (r.active() != null) w.setActive(r.active());
    }

    @PostMapping
    public WardOverview create(@Valid @RequestBody WardRequest req) {
        if (wardRepository.existsByNameIgnoreCase(req.name().trim())) throw ApiException.conflict("A ward with that name already exists.");
        Ward w = new Ward();
        apply(w, req);
        return wardService.overview(wardRepository.save(w));
    }

    @PutMapping("/{id}")
    public WardOverview update(@PathVariable Long id, @Valid @RequestBody WardRequest req) {
        Ward w = wardRepository.findById(id).orElseThrow(() -> ApiException.notFound("Ward not found."));
        apply(w, req);
        return wardService.overview(wardRepository.save(w));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!wardRepository.existsById(id)) throw ApiException.notFound("Ward not found.");
        if (admissionRepository.existsByWardId(id)) {
            throw ApiException.conflict("This ward has admission history. Close it instead of deleting.");
        }
        wardRepository.deleteById(id);
    }
}

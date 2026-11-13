package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.AppointmentRequest;
import com.example.MEDICARE.dto.Requests.AppointmentStatusRequest;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired private AppointmentService appointmentService;

    public record TypeInfo(String value, String label, String group, java.math.BigDecimal fee, List<String> allowedDoctorTypes) {}

    /** Admin: everything. Doctor: their own. Patient: their own. */
    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) Long doctorId,
                                  @RequestParam(required = false) String date,
                                  @RequestParam(required = false) AppointmentStatus status,
                                  @RequestParam(required = false) AppointmentType type) {
        return appointmentService.list(doctorId, date != null ? LocalDate.parse(date) : null, status, type);
    }

    @GetMapping("/my")
    public List<Appointment> mine() {
        return appointmentService.list(null, null, null, null);
    }

    /** Appointment types (with fee + which kind of doctor can perform them) for the booking forms. */
    @GetMapping("/types")
    public List<TypeInfo> types() {
        return Arrays.stream(AppointmentType.values())
                .map(t -> new TypeInfo(t.name(), t.getLabel(), t.getGroup(), t.getDefaultFee(),
                        t.allowedDoctorTypes().stream().map(Enum::name).toList()))
                .toList();
    }

    @PostMapping
    public Appointment create(@RequestBody AppointmentRequest req) {
        return appointmentService.create(req);
    }

    @PutMapping("/{id}/status")
    public Appointment updateStatus(@PathVariable Long id, @RequestBody AppointmentStatusRequest body) {
        return appointmentService.updateStatus(id, body.status());
    }

    @PutMapping("/{id}/cancel")
    public Appointment cancelOwn(@PathVariable Long id) {
        return appointmentService.cancelOwn(id);
    }
}

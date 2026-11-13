package com.example.MEDICARE.service;

import com.example.MEDICARE.dto.Requests.AdministerRequest;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class InventoryService {

    @Autowired private InventoryItemRepository itemRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AdmissionRepository admissionRepository;
    @Autowired private AdministrationRepository administrationRepository;
    @Autowired private PaymentService paymentService;
    @Autowired private AccessService accessService;
    @Autowired private CurrentUser currentUser;

    @Value("${app.timezone:Africa/Lagos}") private String timezone;

    /**
     * Gives a drug / oxygen / consumable to a patient. In ONE transaction this:
     *  1. checks the item (active, not expired) and the patient,
     *  2. takes the quantity out of stock (atomically - it can never go below zero),
     *  3. records who administered what, when and to whom,
     *  4. optionally bills the patient.
     */
    @Transactional
    public Administration administer(AdministerRequest req) {
        if (req.itemId() == null) throw ApiException.badRequest("Choose an item.");
        if (req.patientId() == null) throw ApiException.badRequest("Choose a patient.");
        if (req.quantity() == null || req.quantity() < 1) throw ApiException.badRequest("Quantity must be at least 1.");

        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> ApiException.notFound("Patient not found."));
        accessService.requirePatientAccess(patient.getId());

        InventoryItem item = itemRepository.findById(req.itemId())
                .orElseThrow(() -> ApiException.notFound("Item not found."));
        if (!item.isActive()) throw ApiException.badRequest(item.getName() + " is no longer stocked.");
        if (item.getExpiryDate() != null && item.getExpiryDate().isBefore(LocalDate.now(ZoneId.of(timezone)))) {
            throw ApiException.badRequest(item.getName() + " has expired and can't be administered.");
        }

        Appointment appointment = null;
        if (req.appointmentId() != null) {
            appointment = appointmentRepository.findById(req.appointmentId())
                    .orElseThrow(() -> ApiException.notFound("Appointment not found."));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw ApiException.badRequest("That appointment belongs to a different patient.");
            }
        }
        Admission admission = null;
        if (req.admissionId() != null) {
            admission = admissionRepository.findById(req.admissionId())
                    .orElseThrow(() -> ApiException.notFound("Admission not found."));
            if (!admission.getPatient().getId().equals(patient.getId())) {
                throw ApiException.badRequest("That admission belongs to a different patient.");
            }
        }

        int updated = itemRepository.decrementStock(item.getId(), req.quantity());
        if (updated == 0) {
            InventoryItem fresh = itemRepository.findById(item.getId()).orElse(item);
            throw ApiException.conflict("Not enough " + fresh.getName() + " in stock (only "
                    + fresh.getStockQuantity() + " " + fresh.getUnit() + " left).");
        }
        item = itemRepository.findById(item.getId()).orElseThrow(); // reload after the bulk update

        BigDecimal unitPrice = item.getUnitPrice();
        BigDecimal total = unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(req.quantity()));

        Administration adm = new Administration();
        adm.setItem(item);
        adm.setPatient(patient);
        adm.setAppointment(appointment);
        adm.setAdmission(admission);
        adm.setQuantity(req.quantity());
        adm.setUnitPrice(unitPrice);
        adm.setTotalCost(total);
        adm.setNotes(req.notes());
        adm.setAdministeredBy(currentUser.user().getFullName());
        adm.setAdministeredAt(LocalDateTime.now(ZoneId.of(timezone)));
        adm = administrationRepository.save(adm);

        boolean bill = req.billPatient() == null || req.billPatient();
        if (bill && total != null && total.signum() > 0) {
            paymentService.createCharge(patient, appointment, admission, total,
                    "Administered: " + item.getName() + " x" + req.quantity() + " " + item.getUnit());
        }
        return adm;
    }

    @Transactional
    public InventoryItem restock(Long itemId, int quantity) {
        if (quantity < 1) throw ApiException.badRequest("Restock quantity must be at least 1.");
        if (itemRepository.incrementStock(itemId, quantity) == 0) throw ApiException.notFound("Item not found.");
        return itemRepository.findById(itemId).orElseThrow();
    }
}

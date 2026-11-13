package com.example.MEDICARE.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row each time a drug / oxygen / consumable is given to a patient.
 * Creating one also reduces the stock of the inventory item.
 */
@Entity
@Table(name = "administrations")
public class Administration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "admission_id")
    private Admission admission;

    @Column(nullable = false)
    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalCost;

    @Column(length = 1000)
    private String notes;

    private String administeredBy;

    private LocalDateTime administeredAt = LocalDateTime.now();

    public Administration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem item) { this.item = item; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public Admission getAdmission() { return admission; }
    public void setAdmission(Admission admission) { this.admission = admission; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(String administeredBy) { this.administeredBy = administeredBy; }

    public LocalDateTime getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(LocalDateTime administeredAt) { this.administeredAt = administeredAt; }

}

package com.example.MEDICARE.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "admissions")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    private Integer bedNumber;

    @ManyToOne
    @JoinColumn(name = "attending_doctor_id")
    private Doctor attendingDoctor;

    @Enumerated(EnumType.STRING)
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    @Column(length = 1000)
    private String reasonForAdmission;

    private LocalDateTime admittedAt = LocalDateTime.now();

    private LocalDateTime dischargedAt;

    @Column(length = 2000)
    private String dischargeSummary;

    private String admittedBy;

    public Admission() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Ward getWard() { return ward; }
    public void setWard(Ward ward) { this.ward = ward; }

    public Integer getBedNumber() { return bedNumber; }
    public void setBedNumber(Integer bedNumber) { this.bedNumber = bedNumber; }

    public Doctor getAttendingDoctor() { return attendingDoctor; }
    public void setAttendingDoctor(Doctor attendingDoctor) { this.attendingDoctor = attendingDoctor; }

    public AdmissionStatus getStatus() { return status; }
    public void setStatus(AdmissionStatus status) { this.status = status; }

    public String getReasonForAdmission() { return reasonForAdmission; }
    public void setReasonForAdmission(String reasonForAdmission) { this.reasonForAdmission = reasonForAdmission; }

    public LocalDateTime getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(LocalDateTime admittedAt) { this.admittedAt = admittedAt; }

    public LocalDateTime getDischargedAt() { return dischargedAt; }
    public void setDischargedAt(LocalDateTime dischargedAt) { this.dischargedAt = dischargedAt; }

    public String getDischargeSummary() { return dischargeSummary; }
    public void setDischargeSummary(String dischargeSummary) { this.dischargeSummary = dischargeSummary; }

    public String getAdmittedBy() { return admittedBy; }
    public void setAdmittedBy(String admittedBy) { this.admittedBy = admittedBy; }

}

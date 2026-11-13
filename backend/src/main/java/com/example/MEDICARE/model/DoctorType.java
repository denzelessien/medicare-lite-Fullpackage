package com.example.MEDICARE.model;
import java.math.BigDecimal;
import java.util.Set;


public enum DoctorType {
    GENERAL_PRACTITIONER("General Practitioner"),
    SURGEON("Surgeon"),
    RADIOLOGIST("Radiologist"),
    PEDIATRICIAN("Pediatrician"),
    OBSTETRICIAN_GYNECOLOGIST("Obstetrician / Gynecologist"),
    CARDIOLOGIST("Cardiologist"),
    EMERGENCY_PHYSICIAN("Emergency Physician"),
    ANESTHESIOLOGIST("Anesthesiologist"),
    DENTIST("Dentist"),
    OTHER("Other specialist");
    private final String label;

    DoctorType(String label) { this.label = label; }

    public String getLabel() { return label; }
}

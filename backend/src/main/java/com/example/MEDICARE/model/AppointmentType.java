package com.example.MEDICARE.model;
import java.math.BigDecimal;
import java.util.Set;


public enum AppointmentType {
    CHECKUP("Check-up", "General", 5000),
    CONSULTATION("Consultation", "General", 7500),
    FOLLOW_UP("Follow-up visit", "General", 3000),
    SURGERY("Surgery", "Surgery", 150000),
    XRAY("X-ray", "Radiology", 12000),
    CT_SCAN("CT scan", "Radiology", 45000),
    MRI("MRI scan", "Radiology", 90000),
    ULTRASOUND("Ultrasound", "Radiology", 15000),
    LAB_TEST("Lab test", "Diagnostics", 8000),
    VACCINATION("Vaccination", "General", 4000);
    private final String label;
    private final String group;
    private final int defaultFee;

    AppointmentType(String label, String group, int defaultFee) {
        this.label = label;
        this.group = group;
        this.defaultFee = defaultFee;
    }

    public String getLabel() { return label; }
    public String getGroup() { return group; }
    public BigDecimal getDefaultFee() { return BigDecimal.valueOf(defaultFee); }

    public boolean isRadiology() { return "Radiology".equals(group); }

    /** Doctor types who may perform this kind of appointment. Empty set = any doctor. */
    public Set<DoctorType> allowedDoctorTypes() {
        if (isRadiology()) return Set.of(DoctorType.RADIOLOGIST);
        if (this == SURGERY) return Set.of(DoctorType.SURGEON, DoctorType.OBSTETRICIAN_GYNECOLOGIST);
        return Set.of();
    }
}

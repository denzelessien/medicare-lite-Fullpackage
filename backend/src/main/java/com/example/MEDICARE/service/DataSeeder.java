package com.example.MEDICARE.service;

import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** On first run: creates the first admin, and (optionally) starter wards / pharmacy categories / stock. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WardRepository wardRepository;
    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private InventoryItemRepository itemRepository;

    @Value("${app.seed.admin-name:System Admin}") private String adminName;
    @Value("${app.seed.admin-email:admin@medicare.local}") private String adminEmail;
    @Value("${app.seed.admin-password:Admin@12345}") private String adminPassword;
    @Value("${app.seed.demo-data:true}") private boolean demoData;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User(adminName, adminEmail.toLowerCase(), passwordEncoder.encode(adminPassword), Role.ADMIN);
            admin.setEmailVerified(true);
            userRepository.save(admin);
            log.warn("Created first admin account: {} / {}  (change this password!)", adminEmail, adminPassword);
        }
        if (!demoData) return;

        if (wardRepository.count() == 0) {
            ward("Emergency Ward", WardType.EMERGENCY, 20, 15000, "24-hour emergency and trauma care");
            ward("Maternity Ward", WardType.MATERNITY, 15, 12000, "Antenatal, labour and postnatal care");
            ward("Pediatric Ward", WardType.PEDIATRIC, 12, 10000, "Care for infants and children");
            ward("General Ward", WardType.GENERAL, 30, 8000, "General medical admissions");
            ward("Surgical Ward", WardType.SURGICAL, 15, 18000, "Pre- and post-operative care");
            ward("Intensive Care Unit", WardType.ICU, 8, 60000, "Critical care with continuous monitoring");
        }

        if (categoryRepository.count() == 0) {
            ItemCategory analgesic = cat("Analgesics", "Pain and fever relief");
            ItemCategory antibiotic = cat("Antibiotics", "Bacterial infections");
            ItemCategory antimalarial = cat("Antimalarials", "Malaria treatment");
            ItemCategory gas = cat("Medical Gases", "Oxygen and other medical gases");
            ItemCategory fluids = cat("IV Fluids & Consumables", "Drips, syringes, dressings");

            item("Paracetamol 500mg", ItemType.DRUG, analgesic, "tablets", 500, 100, 20, 2030);
            item("Amoxicillin 500mg", ItemType.DRUG, antibiotic, "capsules", 300, 60, 50, 2028);
            item("Artemether/Lumefantrine", ItemType.DRUG, antimalarial, "tablets", 200, 40, 350, 2028);
            item("Medical Oxygen", ItemType.MEDICAL_GAS, gas, "litres", 5000, 1000, 5, null);
            item("Normal Saline 0.9% (500ml)", ItemType.CONSUMABLE, fluids, "bags", 120, 30, 900, 2028);
        }
    }

    private void ward(String name, WardType type, int capacity, int rate, String desc) {
        Ward w = new Ward();
        w.setName(name); w.setWardType(type); w.setCapacity(capacity);
        w.setDailyRate(BigDecimal.valueOf(rate)); w.setDescription(desc);
        wardRepository.save(w);
    }

    private ItemCategory cat(String name, String desc) {
        ItemCategory c = new ItemCategory();
        c.setName(name); c.setDescription(desc);
        return categoryRepository.save(c);
    }

    private void item(String name, ItemType type, ItemCategory cat, String unit, int stock, int reorder, int price, Integer expiryYear) {
        InventoryItem i = new InventoryItem();
        i.setName(name); i.setItemType(type); i.setCategory(cat); i.setUnit(unit);
        i.setStockQuantity(stock); i.setReorderLevel(reorder); i.setUnitPrice(BigDecimal.valueOf(price));
        if (expiryYear != null) i.setExpiryDate(LocalDate.of(expiryYear, 12, 31));
        i.setSupplier("MediSupply Nigeria Ltd");
        itemRepository.save(i);
    }
}

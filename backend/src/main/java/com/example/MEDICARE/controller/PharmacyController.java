package com.example.MEDICARE.controller;

import com.example.MEDICARE.dto.Requests.*;
import com.example.MEDICARE.exception.ApiException;
import com.example.MEDICARE.model.*;
import com.example.MEDICARE.repository.*;
import com.example.MEDICARE.service.CurrentUser;
import com.example.MEDICARE.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pharmacy & inventory. Admins manage categories/items; admins AND doctors can administer
 * an item to a patient (which automatically reduces stock).
 */
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private InventoryItemRepository itemRepository;
    @Autowired private AdministrationRepository administrationRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private CurrentUser currentUser;

    // ---------------- categories ----------------

    @GetMapping("/categories")
    public List<ItemCategory> categories() { return categoryRepository.findAll(); }

    @PostMapping("/categories")
    public ItemCategory createCategory(@Valid @RequestBody CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.name().trim())) throw ApiException.conflict("That category already exists.");
        ItemCategory c = new ItemCategory();
        c.setName(req.name().trim());
        c.setDescription(req.description());
        return categoryRepository.save(c);
    }

    @PutMapping("/categories/{id}")
    public ItemCategory updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        ItemCategory c = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category not found."));
        c.setName(req.name().trim());
        c.setDescription(req.description());
        return categoryRepository.save(c);
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) throw ApiException.notFound("Category not found.");
        if (itemRepository.existsByCategoryId(id)) throw ApiException.conflict("Move or remove the items in this category first.");
        categoryRepository.deleteById(id);
    }

    // ---------------- items ----------------

    @GetMapping("/items")
    public List<InventoryItem> items(@RequestParam(defaultValue = "false") boolean all) {
        // Only admins can see items that were retired from stock.
        return all && currentUser.isAdmin() ? itemRepository.findAll() : itemRepository.findByActiveTrueOrderByNameAsc();
    }

    private void apply(InventoryItem i, ItemRequest r) {
        i.setName(r.name().trim());
        i.setItemType(r.itemType() != null ? r.itemType() : ItemType.DRUG);
        i.setCategory(r.categoryId() == null ? null
                : categoryRepository.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Unknown category.")));
        i.setUnit(r.unit() == null || r.unit().isBlank() ? "units" : r.unit().trim());
        if (r.stockQuantity() != null) {
            if (r.stockQuantity() < 0) throw ApiException.badRequest("Stock can't be negative.");
            i.setStockQuantity(r.stockQuantity());
        }
        i.setReorderLevel(r.reorderLevel() != null ? Math.max(0, r.reorderLevel()) : 10);
        i.setUnitPrice(r.unitPrice());
        i.setExpiryDate(r.expiryDate());
        i.setSupplier(r.supplier());
    }

    @PostMapping("/items")
    public InventoryItem createItem(@Valid @RequestBody ItemRequest req) {
        InventoryItem i = new InventoryItem();
        apply(i, req);
        return itemRepository.save(i);
    }

    @PutMapping("/items/{id}")
    public InventoryItem updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest req) {
        InventoryItem i = itemRepository.findById(id).orElseThrow(() -> ApiException.notFound("Item not found."));
        apply(i, req);
        return itemRepository.save(i);
    }

    /** Retires an item (history of what was administered is kept, so we don't hard-delete). */
    @DeleteMapping("/items/{id}")
    public void retireItem(@PathVariable Long id) {
        InventoryItem i = itemRepository.findById(id).orElseThrow(() -> ApiException.notFound("Item not found."));
        i.setActive(false);
        itemRepository.save(i);
    }

    @PostMapping("/items/{id}/restock")
    public InventoryItem restock(@PathVariable Long id, @RequestBody RestockRequest req) {
        return inventoryService.restock(id, req.quantity() == null ? 0 : req.quantity());
    }

    // ---------------- administering ----------------

    @PostMapping("/administrations")
    public Administration administer(@RequestBody AdministerRequest req) {
        return inventoryService.administer(req);
    }

    @GetMapping("/administrations")
    public List<Administration> administrations(@RequestParam(required = false) Long patientId) {
        return patientId != null
                ? administrationRepository.findByPatientIdOrderByAdministeredAtDesc(patientId)
                : administrationRepository.findTop100ByOrderByAdministeredAtDesc();
    }
}

package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}

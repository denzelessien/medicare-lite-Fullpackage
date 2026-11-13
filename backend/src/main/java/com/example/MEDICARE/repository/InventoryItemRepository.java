package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByActiveTrueOrderByNameAsc();
    boolean existsByCategoryId(Long categoryId);

    /** Atomic "take stock": only succeeds when enough is left, so stock can never go negative. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.stockQuantity = i.stockQuantity - :qty " +
           "where i.id = :id and i.active = true and i.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.stockQuantity = i.stockQuantity + :qty where i.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}

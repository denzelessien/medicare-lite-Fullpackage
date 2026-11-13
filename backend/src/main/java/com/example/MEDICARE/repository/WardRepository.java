package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Ward;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {
    boolean existsByNameIgnoreCase(String name);

    /** Row lock so two people can't take the last bed at the same moment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Ward w where w.id = :id")
    Optional<Ward> findByIdForUpdate(@Param("id") Long id);
}

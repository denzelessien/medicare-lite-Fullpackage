package com.example.MEDICARE.repository;

import com.example.MEDICARE.model.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NurseRepository extends JpaRepository<Nurse, Long> {
}

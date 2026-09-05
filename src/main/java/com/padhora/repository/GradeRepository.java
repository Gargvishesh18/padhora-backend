package com.padhora.repository;

import com.padhora.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByActiveTrueOrderBySortOrderAsc();
    Optional<Grade> findBySlug(String slug);
}

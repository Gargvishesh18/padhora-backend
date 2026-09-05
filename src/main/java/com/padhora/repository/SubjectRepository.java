package com.padhora.repository;

import com.padhora.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByActiveTrueOrderBySortOrderAsc();
    Optional<Subject> findBySlug(String slug);
}

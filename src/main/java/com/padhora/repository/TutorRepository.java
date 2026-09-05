package com.padhora.repository;

import com.padhora.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByEmail(String email);
    Optional<Tutor> findByAuthPhone(String authPhone);

    // Base candidate fetch for search: status + the filters that can be expressed as an exact
    // match or a join. Distance filtering and ranking happen afterwards in
    // TutorSearchService, since they need per-request lat/lng rather than a fixed predicate.
    //
    // gradeSlug/subjectSlug join the normalised tutor_grades/tutor_subjects tables (see
    // migration V6+V7) - this is what lets search filter by class and subject at all, which
    // area/mode/type alone never could.
    @Query("""
        SELECT DISTINCT t FROM Tutor t
        LEFT JOIN t.types ty
        LEFT JOIN t.modes md
        LEFT JOIN t.grades gr
        LEFT JOIN t.subjects sub
        WHERE t.status = 'APPROVED'
        AND (:area IS NULL OR t.area = :area)
        AND (:mode IS NULL OR md = :mode)
        AND (:type IS NULL OR ty = :type)
        AND (:gradeSlug IS NULL OR gr.slug = :gradeSlug)
        AND (:subjectSlug IS NULL OR sub.slug = :subjectSlug)
        """)
    List<Tutor> search(@Param("area") String area,
                        @Param("mode") String mode,
                        @Param("type") String type,
                        @Param("gradeSlug") String gradeSlug,
                        @Param("subjectSlug") String subjectSlug);

    List<Tutor> findByStatus(Tutor.Status status);
}

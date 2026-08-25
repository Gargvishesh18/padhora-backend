package com.padhora.repository;

import com.padhora.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TutorRepository extends JpaRepository<Tutor, Long> {

    @Query("""
        SELECT DISTINCT t FROM Tutor t
        LEFT JOIN t.types ty
        LEFT JOIN t.modes md
        WHERE t.status = 'APPROVED'
        AND (:area IS NULL OR t.area = :area)
        AND (:mode IS NULL OR md = :mode)
        AND (:type IS NULL OR ty = :type)
        ORDER BY t.createdAt DESC
        """)
    List<Tutor> search(@Param("area") String area,
                        @Param("mode") String mode,
                        @Param("type") String type);

    List<Tutor> findByStatus(Tutor.Status status);
}

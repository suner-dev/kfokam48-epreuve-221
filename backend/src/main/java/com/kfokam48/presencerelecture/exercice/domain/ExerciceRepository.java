package com.kfokam48.presencerelecture.exercice.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {
    Optional<Exercice> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
    List<Exercice> findBySessionIdOrderById(Long sessionId);

    @Query("""
            select exercise.etudiantId, count(exercise)
            from Exercice exercise, SessionCours session
            where exercise.sessionId = session.id
              and session.promotionId = :promotionId
            group by exercise.etudiantId
            """)
    List<Object[]> countByPromotion(@Param("promotionId") Long promotionId);
}

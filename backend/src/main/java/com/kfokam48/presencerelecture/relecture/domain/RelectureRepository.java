package com.kfokam48.presencerelecture.relecture.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {
    Optional<Relecture> findByExerciceId(Long exerciceId);
    List<Relecture> findByExerciceIdIn(List<Long> exerciceIds);

    @Query("""
            select review
            from Relecture review, Exercice exercise
            where review.exerciceId = exercise.id
              and exercise.etudiantId = :etudiantId
              and review.rendueAt is not null
            order by review.rendueAt desc
            """)
    List<Relecture> findRenderedForAuthor(@Param("etudiantId") Long etudiantId);

    @Query("""
            select review.relecteurId, count(review)
            from Relecture review, Exercice exercise, SessionCours session
            where review.exerciceId = exercise.id
              and exercise.sessionId = session.id
              and session.promotionId = :promotionId
              and review.relecteurId is not null
              and review.rendueAt is null
            group by review.relecteurId
            """)
    List<Object[]> countPendingByPromotion(@Param("promotionId") Long promotionId);

    @Query("""
            select exercise.etudiantId, avg(review.note)
            from Relecture review, Exercice exercise, SessionCours session
            where review.exerciceId = exercise.id
              and exercise.sessionId = session.id
              and session.promotionId = :promotionId
              and review.rendueAt is not null
            group by exercise.etudiantId
            """)
    List<Object[]> averageByPromotion(@Param("promotionId") Long promotionId);
}

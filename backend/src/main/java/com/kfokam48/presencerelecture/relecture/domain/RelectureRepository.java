package com.kfokam48.presencerelecture.relecture.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {
    List<Relecture> findByExerciceIdOrderById(Long exerciceId);
    List<Relecture> findByExerciceIdIn(List<Long> exerciceIds);
    Optional<Relecture> findByExerciceIdAndRelecteurId(Long exerciceId, Long relecteurId);

    @Query("""
            select exercise
            from Exercice exercise
            where exercise.etudiantId = :etudiantId
              and exists (select review.id from Relecture review
                          where review.exerciceId = exercise.id and review.rendueAt is not null)
            order by (select max(rendue.rendueAt) from Relecture rendue
                      where rendue.exerciceId = exercise.id) desc, exercise.id
            """)
    List<com.kfokam48.presencerelecture.exercice.domain.Exercice> findReviewedExercisesForAuthor(
            @Param("etudiantId") Long etudiantId);

    @Query("""
            select review
            from Relecture review
            where review.exerciceId = :exerciceId
              and review.rendueAt is not null
            order by review.id
            """)
    List<Relecture> findRenderedByExercise(@Param("exerciceId") Long exerciceId);

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
            select exercise.etudiantId, avg(noteMoyenne)
            from (
                select exercise.id as exerciceId, exercise.etudiantId as etudiantId,
                       avg(review.note) as noteMoyenne
                from Relecture review, Exercice exercise, SessionCours session
                where review.exerciceId = exercise.id
                  and exercise.sessionId = session.id
                  and session.promotionId = :promotionId
                  and review.rendueAt is not null
                group by exercise.id, exercise.etudiantId
            ) exercise
            group by exercise.etudiantId
            """)
    List<Object[]> averageByPromotion(@Param("promotionId") Long promotionId);

    @Query("""
            select review
            from Relecture review
            where review.relecteurId = :etudiantId
              and review.rendueAt is null
            order by review.id
            """)
    List<Relecture> findPendingForReviewer(@Param("etudiantId") Long etudiantId);
}

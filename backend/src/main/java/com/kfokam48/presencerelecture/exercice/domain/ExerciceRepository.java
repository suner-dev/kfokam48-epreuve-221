package com.kfokam48.presencerelecture.exercice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {
    Optional<Exercice> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}

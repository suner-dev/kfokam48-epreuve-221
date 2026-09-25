package com.kfokam48.presencerelecture.relecture.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {
    Optional<Relecture> findByExerciceId(Long exerciceId);
}

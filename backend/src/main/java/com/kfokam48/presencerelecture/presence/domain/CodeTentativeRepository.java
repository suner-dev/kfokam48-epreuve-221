package com.kfokam48.presencerelecture.presence.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeTentativeRepository extends JpaRepository<CodeTentative, Long> {
    Optional<CodeTentative> findByEtudiantId(Long etudiantId);
}

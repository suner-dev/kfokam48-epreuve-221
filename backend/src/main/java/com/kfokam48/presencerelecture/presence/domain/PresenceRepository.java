package com.kfokam48.presencerelecture.presence.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    Optional<Presence> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}

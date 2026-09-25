package com.kfokam48.presencerelecture.presence.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
    Optional<Presence> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
    List<Presence> findBySessionId(Long sessionId);

    @Query("""
            select presence.etudiantId, count(presence)
            from Presence presence, SessionCours session
            where presence.sessionId = session.id
              and session.promotionId = :promotionId
            group by presence.etudiantId
            """)
    List<Object[]> countByPromotion(@Param("promotionId") Long promotionId);
}

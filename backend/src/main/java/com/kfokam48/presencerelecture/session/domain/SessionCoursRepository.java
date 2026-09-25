package com.kfokam48.presencerelecture.session.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionCoursRepository extends JpaRepository<SessionCours, Long> {
    Optional<SessionCours> findByCode(String code);
    List<SessionCours> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);
}

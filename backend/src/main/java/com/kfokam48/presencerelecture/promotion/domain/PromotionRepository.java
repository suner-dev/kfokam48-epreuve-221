package com.kfokam48.presencerelecture.promotion.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByNom(String nom);
}

package com.kfokam48.presencerelecture.etudiant.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    List<Etudiant> findByPromotionIdOrderByNomAscPrenomAsc(Long promotionId);
    Optional<Etudiant> findByIdAndPromotionId(Long id, Long promotionId);
    long countByPromotionId(Long promotionId);
}

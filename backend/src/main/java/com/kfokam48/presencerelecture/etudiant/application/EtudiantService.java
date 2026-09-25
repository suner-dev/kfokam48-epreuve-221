package com.kfokam48.presencerelecture.etudiant.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EtudiantService {
    private final EtudiantRepository repository;

    public EtudiantService(EtudiantRepository repository) {
        this.repository = repository;
    }

    public List<Etudiant> findByPromotion(Long promotionId) {
        return repository.findByPromotionIdOrderByNomAscPrenomAsc(promotionId);
    }

    public Etudiant require(Long id, Long promotionId) {
        return repository.findByIdAndPromotionId(id, promotionId).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "ETUDIANT_INCONNU",
                "L'étudiant demandé n'existe pas dans cette promotion."
        ));
    }

    public Etudiant require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "ETUDIANT_INCONNU",
                "L'étudiant demandé n'existe pas."
        ));
    }
}

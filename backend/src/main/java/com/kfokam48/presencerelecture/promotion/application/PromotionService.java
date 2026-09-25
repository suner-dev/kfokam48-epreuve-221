package com.kfokam48.presencerelecture.promotion.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.promotion.domain.Promotion;
import com.kfokam48.presencerelecture.promotion.domain.PromotionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PromotionService {
    private final PromotionRepository repository;

    public PromotionService(PromotionRepository repository) {
        this.repository = repository;
    }

    public List<Promotion> findAll() {
        return repository.findAll();
    }

    public Promotion require(Long id) {
        return require(id, HttpStatus.NOT_FOUND);
    }

    /**
     * Le contrat n'autorise que 400 sur les écritures imposées : quand le promotionId arrive
     * dans le corps de la requête (POST /api/sessions), l'absence est une erreur de requête (400),
     * pas une ressource introuvable (404).
     */
    public Promotion require(Long id, HttpStatus statutAbsence) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                statutAbsence,
                "PROMOTION_INCONNUE",
                "La promotion demandée n'existe pas."
        ));
    }
}

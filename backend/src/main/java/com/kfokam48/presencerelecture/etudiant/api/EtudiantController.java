package com.kfokam48.presencerelecture.etudiant.api;

import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.relecture.api.ReceivedReviewResponse;
import com.kfokam48.presencerelecture.relecture.application.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {
    private final EtudiantService service;
    private final ReviewService reviewService;

    public EtudiantController(EtudiantService service, ReviewService reviewService) {
        this.service = service;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<EtudiantResponse> findByPromotion(@RequestParam Long promotionId) {
        return service.findByPromotion(promotionId).stream()
                .map(student -> new EtudiantResponse(student.getId(), student.getNom(), student.getPrenom()))
                .toList();
    }

    @GetMapping("/{etudiantId}/relectures-recues")
    public List<ReceivedReviewResponse> received(@PathVariable Long etudiantId) {
        return reviewService.received(etudiantId);
    }
}

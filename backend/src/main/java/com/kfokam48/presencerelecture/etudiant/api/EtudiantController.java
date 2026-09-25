package com.kfokam48.presencerelecture.etudiant.api;

import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.relecture.api.ReceivedReviewResponse;
import com.kfokam48.presencerelecture.relecture.application.ReceivedReviewService;
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
    private final ReceivedReviewService receivedReviewService;

    public EtudiantController(EtudiantService service, ReceivedReviewService receivedReviewService) {
        this.service = service;
        this.receivedReviewService = receivedReviewService;
    }

    @GetMapping
    public List<EtudiantResponse> findByPromotion(@RequestParam Long promotionId) {
        return service.findByPromotion(promotionId).stream()
                .map(student -> new EtudiantResponse(student.getId(), student.getNom(), student.getPrenom()))
                .toList();
    }

    @GetMapping("/{id}/relectures-recues")
    public List<ReceivedReviewResponse> receivedReviews(@PathVariable Long id) {
        return receivedReviewService.findForAuthor(id);
    }
}

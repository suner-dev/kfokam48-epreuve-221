package com.kfokam48.presencerelecture.etudiant.api;

import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {
    private final EtudiantService service;

    public EtudiantController(EtudiantService service) {
        this.service = service;
    }

    @GetMapping
    public List<EtudiantResponse> findByPromotion(@RequestParam Long promotionId) {
        return service.findByPromotion(promotionId).stream()
                .map(student -> new EtudiantResponse(student.getId(), student.getNom(), student.getPrenom()))
                .toList();
    }
}

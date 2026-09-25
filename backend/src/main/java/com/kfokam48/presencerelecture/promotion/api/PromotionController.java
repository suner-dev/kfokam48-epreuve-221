package com.kfokam48.presencerelecture.promotion.api;

import com.kfokam48.presencerelecture.promotion.application.PromotionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromotionResponse> findAll() {
        return service.findAll().stream()
                .map(promotion -> new PromotionResponse(promotion.getId(), promotion.getNom()))
                .toList();
    }
}

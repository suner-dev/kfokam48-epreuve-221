package com.kfokam48.presencerelecture.tableau.api;

import com.kfokam48.presencerelecture.tableau.application.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tableau")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public List<DashboardRow> findByPromotion(@RequestParam Long promotionId) {
        return service.findByPromotion(promotionId);
    }
}
